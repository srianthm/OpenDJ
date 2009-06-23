package org.opends.common.api.extended;

import org.opends.server.util.Validator;
import org.opends.server.protocols.ldap.LDAPMessage;
import org.opends.server.types.ByteString;
import org.opends.common.api.raw.request.RawRequest;
import org.opends.common.api.raw.RawMessage;

/**
 * Created by IntelliJ IDEA. User: digitalperk Date: Jun 19, 2009 Time: 8:39:31
 * PM To change this template use File | Settings | File Templates.
 */
public abstract class ExtendedRequest<T extends ExtendedOperation> 
    extends RawMessage implements RawRequest
{
  // The extended request name OID.
  protected String requestName;

  /**
   * Creates a new raw extended request using the provided OID.
   * <p>
   * The new raw extended request will contain an empty list of
   * controls, and no value.
   *
   * @param requestName
   *          The extended request name OID.
   */
  protected ExtendedRequest(String requestName)
  {
    Validator.ensureNotNull(requestName);
    this.requestName = requestName;
  }

  public abstract T getExtendedOperation();

  /**
   * Get the request name OID of this extended request.
   *
   * @return The response name OID.
   */
  public String getRequestName()
  {
    return requestName;
  }

  /**
   * Get the response value of this intermediate response or
   * <code>NULL</code> if it is not available.
   *
   * @return the response value or <code>NULL</code>.
   */
  public abstract ByteString getRequestValue();
}
