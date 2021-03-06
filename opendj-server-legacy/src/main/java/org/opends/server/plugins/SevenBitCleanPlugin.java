/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2008 Sun Microsystems, Inc.
 *      Portions Copyright 2014-2015 ForgeRock AS
 */
package org.opends.server.plugins;

import static org.opends.messages.PluginMessages.*;

import java.util.List;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.meta.PluginCfgDefn;
import org.opends.server.admin.std.server.SevenBitCleanPluginCfg;
import org.opends.server.admin.std.server.PluginCfg;
import org.opends.server.api.plugin.DirectoryServerPlugin;
import org.opends.server.api.plugin.PluginResult;
import org.opends.server.api.plugin.PluginType;
import org.forgerock.opendj.config.server.ConfigChangeResult;
import org.forgerock.opendj.config.server.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.*;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ByteSequence;
import org.opends.server.types.operation.PreParseAddOperation;
import org.opends.server.types.operation.PreParseModifyOperation;
import org.opends.server.types.operation.PreParseModifyDNOperation;

/**
 * This class implements a Directory Server plugin that can be used to ensure
 * that the values for a specified set of attributes (optionally, below a
 * specified set of base DNs) are 7-bit clean (i.e., contain only ASCII
 * characters).
 */
public final class SevenBitCleanPlugin
       extends DirectoryServerPlugin<SevenBitCleanPluginCfg>
       implements ConfigurationChangeListener<SevenBitCleanPluginCfg>
{
  /** The bitmask that will be used to make the comparisons. */
  private static final byte MASK = 0x7F;

  /** The current configuration for this plugin. */
  private SevenBitCleanPluginCfg currentConfig;

  /**
   * Creates a new instance of this Directory Server plugin.  Every plugin must
   * implement a default constructor (it is the only one that will be used to
   * create plugins defined in the configuration), and every plugin constructor
   * must call {@code super()} as its first element.
   */
  public SevenBitCleanPlugin()
  {
    super();
  }



  /** {@inheritDoc} */
  @Override
  public final void initializePlugin(Set<PluginType> pluginTypes,
                                     SevenBitCleanPluginCfg configuration)
         throws ConfigException
  {
    currentConfig = configuration;
    configuration.addSevenBitCleanChangeListener(this);

    // Make sure that the plugin has been enabled for the appropriate types.
    for (PluginType t : pluginTypes)
    {
      switch (t)
      {
        case LDIF_IMPORT:
        case PRE_PARSE_ADD:
        case PRE_PARSE_MODIFY:
        case PRE_PARSE_MODIFY_DN:
          // These are acceptable.
          break;

        default:
          throw new ConfigException(ERR_PLUGIN_7BIT_INVALID_PLUGIN_TYPE.get(t));
      }
    }
  }



  /** {@inheritDoc} */
  @Override
  public final void finalizePlugin()
  {
    currentConfig.removeSevenBitCleanChangeListener(this);
  }



  /** {@inheritDoc} */
  @Override
  public final PluginResult.ImportLDIF
               doLDIFImport(LDIFImportConfig importConfig, Entry entry)
  {
    // Get the current configuration for this plugin.
    SevenBitCleanPluginCfg config = currentConfig;


    // Make sure that the entry is within the scope of this plugin.  While
    // processing an LDIF import, we don't have access to the set of public
    // naming contexts defined in the server, so if no explicit set of base DNs
    // is defined, then assume that the entry is in scope.
    if (!isDescendantOfAny(entry.getName(), config.getBaseDN()))
    {
      // The entry is out of scope, so we won't process it.
      return PluginResult.ImportLDIF.continueEntryProcessing();
    }


    // Make sure all configured attributes have clean values.
    for (AttributeType t : config.getAttributeType())
    {
      List<Attribute> attrList = entry.getAttribute(t);
      if (attrList != null)
      {
        for (Attribute a : attrList)
        {
          for (ByteString v : a)
          {
            if (!is7BitClean(v))
            {
              LocalizableMessage rejectMessage =
                   ERR_PLUGIN_7BIT_IMPORT_ATTR_NOT_CLEAN.get(a.getNameWithOptions());
              return PluginResult.ImportLDIF.stopEntryProcessing(rejectMessage);
            }
          }
        }
      }
    }


    // If we've gotten here, then everything is acceptable.
    return PluginResult.ImportLDIF.continueEntryProcessing();
  }



  /** {@inheritDoc} */
  @Override
  public final PluginResult.PreParse
               doPreParse(PreParseAddOperation addOperation)
  {
    // Get the current configuration for this plugin.
    SevenBitCleanPluginCfg config = currentConfig;


    // If the entry is within the scope of this plugin, then make sure all
    // configured attributes have clean values.
    DN entryDN;
    try
    {
      entryDN = DN.decode(addOperation.getRawEntryDN());
    }
    catch (DirectoryException de)
    {
      return PluginResult.PreParse.stopProcessing(de.getResultCode(),
          ERR_PLUGIN_7BIT_CANNOT_DECODE_DN.get(de.getMessageObject()));
    }

    if (isInScope(config, entryDN))
    {
      for (RawAttribute rawAttr : addOperation.getRawAttributes())
      {
        Attribute a;
        try
        {
          a = rawAttr.toAttribute();
        }
        catch (LDAPException le)
        {
          return PluginResult.PreParse.stopProcessing(
              ResultCode.valueOf(le.getResultCode()),
              ERR_PLUGIN_7BIT_CANNOT_DECODE_ATTR.get(
                  rawAttr.getAttributeType(), le.getErrorMessage()));
        }

        if (! config.getAttributeType().contains(a.getAttributeType()))
        {
          continue;
        }

        for (ByteString v : a)
        {
          if (!is7BitClean(v))
          {
            return PluginResult.PreParse.stopProcessing(
                ResultCode.CONSTRAINT_VIOLATION,
                ERR_PLUGIN_7BIT_MODIFYDN_ATTR_NOT_CLEAN.get(
                    rawAttr.getAttributeType()));
          }
        }
      }
    }


    // If we've gotten here, then everything is acceptable.
    return PluginResult.PreParse.continueOperationProcessing();
  }



  /** {@inheritDoc} */
  @Override
  public final PluginResult.PreParse
                    doPreParse(PreParseModifyOperation modifyOperation)
  {
    // Get the current configuration for this plugin.
    SevenBitCleanPluginCfg config = currentConfig;


    // If the target entry is within the scope of this plugin, then make sure
    // all values that will be added during the modification will be acceptable.
    DN entryDN;
    try
    {
      entryDN = DN.decode(modifyOperation.getRawEntryDN());
    }
    catch (DirectoryException de)
    {
      return PluginResult.PreParse.stopProcessing(de.getResultCode(),
          ERR_PLUGIN_7BIT_CANNOT_DECODE_DN.get(de.getMessageObject()));
    }

    if (isInScope(config, entryDN))
    {
      for (RawModification m : modifyOperation.getRawModifications())
      {
        switch (m.getModificationType().asEnum())
        {
          case ADD:
          case REPLACE:
            // These are modification types that we will process.
            break;
          default:
            // This is not a modification type that we will process.
            continue;
        }

        RawAttribute rawAttr = m.getAttribute();
        Attribute a;
        try
        {
          a = rawAttr.toAttribute();
        }
        catch (LDAPException le)
        {
          return PluginResult.PreParse.stopProcessing(
              ResultCode.valueOf(le.getResultCode()),
              ERR_PLUGIN_7BIT_CANNOT_DECODE_ATTR.get(
                  rawAttr.getAttributeType(), le.getErrorMessage()));
        }

        if (! config.getAttributeType().contains(a.getAttributeType()))
        {
          continue;
        }

        for (ByteString v : a)
        {
          if (!is7BitClean(v))
          {
            return PluginResult.PreParse.stopProcessing(
                ResultCode.CONSTRAINT_VIOLATION,
                ERR_PLUGIN_7BIT_MODIFYDN_ATTR_NOT_CLEAN.get(
                    rawAttr.getAttributeType()));
          }
        }
      }
    }


    // If we've gotten here, then everything is acceptable.
    return PluginResult.PreParse.continueOperationProcessing();
  }



  /** {@inheritDoc} */
  @Override
  public final PluginResult.PreParse
                    doPreParse(PreParseModifyDNOperation modifyDNOperation)
  {
    // Get the current configuration for this plugin.
    SevenBitCleanPluginCfg config = currentConfig;


    // If the target entry is within the scope of this plugin, then make sure
    // all values that will be added during the modification will be acceptable.
    DN entryDN;
    try
    {
      entryDN = DN.decode(modifyDNOperation.getRawEntryDN());
    }
    catch (DirectoryException de)
    {
      return PluginResult.PreParse.stopProcessing(de.getResultCode(),
          ERR_PLUGIN_7BIT_CANNOT_DECODE_DN.get(de.getMessageObject()));
    }

    if (isInScope(config, entryDN))
    {
      ByteString rawNewRDN = modifyDNOperation.getRawNewRDN();

      RDN newRDN;
      try
      {
        newRDN = RDN.decode(rawNewRDN.toString());
      }
      catch (DirectoryException de)
      {
        return PluginResult.PreParse.stopProcessing(de.getResultCode(),
            ERR_PLUGIN_7BIT_CANNOT_DECODE_NEW_RDN.get(de.getMessageObject()));
      }

      int numValues = newRDN.getNumValues();
      for (int i=0; i < numValues; i++)
      {
        if (! config.getAttributeType().contains(newRDN.getAttributeType(i)))
        {
          continue;
        }

        if (!is7BitClean(newRDN.getAttributeValue(i)))
        {
          return PluginResult.PreParse.stopProcessing(
              ResultCode.CONSTRAINT_VIOLATION,
              ERR_PLUGIN_7BIT_MODIFYDN_ATTR_NOT_CLEAN.get(
                  newRDN.getAttributeName(i)));
        }
      }
    }


    // If we've gotten here, then everything is acceptable.
    return PluginResult.PreParse.continueOperationProcessing();
  }



  /**
   * Indicates whether the provided DN is within the scope of this plugin.
   *
   * @param  config  The configuration to use when making the determination.
   * @param  dn      The DN for which to make the determination.
   *
   * @return  {@code true} if the provided DN is within the scope of this
   *          plugin, or {@code false} if  not.
   */
  private final boolean isInScope(SevenBitCleanPluginCfg config, DN dn)
  {
    Set<DN> baseDNs = config.getBaseDN();
    if (baseDNs == null || baseDNs.isEmpty())
    {
      baseDNs = DirectoryServer.getPublicNamingContexts().keySet();
    }
    return isDescendantOfAny(dn, baseDNs);
  }

  private boolean isDescendantOfAny(DN dn, Set<DN> baseDNs)
  {
    if (baseDNs != null)
    {
      for (DN baseDN: baseDNs)
      {
        if (dn.isDescendantOf(baseDN))
        {
          return true;
        }
      }
    }
    return false;
  }



  /**
   * Indicates whether the provided value is 7-bit clean.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return {@code true} if the provided value is 7-bit clean, or {@code false}
   *         if it is not.
   */
  private final boolean is7BitClean(ByteSequence value)
  {
    for (int i = 0; i < value.length(); i++)
    {
      byte b = value.byteAt(i);
      if ((b & MASK) != b)
      {
        return false;
      }
    }
    return true;
  }



  /** {@inheritDoc} */
  @Override
  public boolean isConfigurationAcceptable(PluginCfg configuration,
                                           List<LocalizableMessage> unacceptableReasons)
  {
    SevenBitCleanPluginCfg cfg = (SevenBitCleanPluginCfg) configuration;
    return isConfigurationChangeAcceptable(cfg, unacceptableReasons);
  }



  /** {@inheritDoc} */
  @Override
  public boolean isConfigurationChangeAcceptable(
                      SevenBitCleanPluginCfg configuration,
                      List<LocalizableMessage> unacceptableReasons)
  {
    boolean configAcceptable = true;

    // Ensure that the set of plugin types is acceptable.
    for (PluginCfgDefn.PluginType pluginType : configuration.getPluginType())
    {
      switch (pluginType)
      {
        case LDIFIMPORT:
        case PREPARSEADD:
        case PREPARSEMODIFY:
        case PREPARSEMODIFYDN:
          // These are acceptable.
          break;


        default:
          unacceptableReasons.add(ERR_PLUGIN_7BIT_INVALID_PLUGIN_TYPE.get(pluginType));
          configAcceptable = false;
      }
    }

    return configAcceptable;
  }



  /** {@inheritDoc} */
  @Override
  public ConfigChangeResult applyConfigurationChange(
                                 SevenBitCleanPluginCfg configuration)
  {
    currentConfig = configuration;
    return new ConfigChangeResult();
  }
}
