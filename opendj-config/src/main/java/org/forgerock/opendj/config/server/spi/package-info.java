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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */

/**
 * Contains a number of API declarations for use throughout the
 * Directory Server.  Whenever possible, these APIs should be declared
 * using abstract classes rather than interfaces so that they can be
 * altered in future versions without impacting backward
 * compatibility.
 * <BR><BR>
 * Note that the mere existence of a class or interface in this
 * package does not in itself imply that it is intended for use by
 * third party code.  Please refer to the official product
 * documentation to indicate which APIs may be safely used by anyone
 * other than the core Directory Server developers.  Failure to heed
 * this warning may result in code that could have unintended side
 * effects or that does not work properly across different Directory
 * Server versions.
 */
package org.forgerock.opendj.config.server.spi;

