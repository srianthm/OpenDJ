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
 */
package org.opends.server.extensions;



import org.opends.server.admin.server.AdminTestCaseUtils;
import org.opends.server.admin.std.meta.TripleDESPasswordStorageSchemeCfgDefn;
import org.opends.server.admin.std.server.TripleDESPasswordStorageSchemeCfg;
import org.opends.server.api.PasswordStorageScheme;



/**
 * A set of test cases for the 3DES password storage scheme.
 */
public class TripleDESPasswordStorageSchemeTestCase
       extends PasswordStorageSchemeTestCase
{
  /**
   * Creates a new instance of this storage scheme test case.
   */
  public TripleDESPasswordStorageSchemeTestCase()
  {
    super("cn=3DES,cn=Password Storage Schemes,cn=config");
  }



  /**
   * Retrieves an initialized instance of this password storage scheme.
   *
   * @return  An initialized instance of this password storage scheme.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected PasswordStorageScheme getScheme()
         throws Exception
  {
    TripleDESPasswordStorageScheme scheme =
         new TripleDESPasswordStorageScheme();

    TripleDESPasswordStorageSchemeCfg configuration =
      AdminTestCaseUtils.getConfiguration(
          TripleDESPasswordStorageSchemeCfgDefn.getInstance(),
          configEntry.getEntry());

    scheme.initializePasswordStorageScheme(configuration);
    return scheme;
  }
}

