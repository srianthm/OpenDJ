/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.opends.sdk.ldif;



import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.opends.sdk.Entry;



/**
 * An interface for writing entries to a data source, typically an LDIF
 * file.
 * <p>
 * TODO: FilteredChangeRecordWriter
 */
public interface EntryWriter extends Closeable, Flushable
{
  /**
   * Closes this entry writer, flushing it first. Closing a previously
   * closed entry writer has no effect.
   *
   * @throws IOException
   *           If an error occurs while closing.
   */
  void close() throws IOException;



  /**
   * Flushes this entry writer so that any buffered data is written
   * immediately to underlying stream, flushing the stream if it is also
   * {@code Flushable}.
   * <p>
   * If the intended destination of this stream is an abstraction
   * provided by the underlying operating system, for example a file,
   * then flushing the stream guarantees only that bytes previously
   * written to the stream are passed to the operating system for
   * writing; it does not guarantee that they are actually written to a
   * physical device such as a disk drive.
   *
   * @throws IOException
   *           If an error occurs while flushing.
   */
  void flush() throws IOException;



  /**
   * Writes a comment.
   *
   * @param comment
   *          The {@code CharSequence} to be written as a comment.
   * @return A reference to this entry writer.
   * @throws IOException
   *           If an error occurs while writing the comment.
   * @throws NullPointerException
   *           If {@code comment} was {@code null}.
   */
  EntryWriter writeComment(CharSequence comment) throws IOException,
      NullPointerException;



  /**
   * Writes an entry.
   *
   * @param entry
   *          The {@code Entry} to be written.
   * @return A reference to this entry writer.
   * @throws IOException
   *           If an error occurs while writing the entry.
   * @throws NullPointerException
   *           If {@code entry} was {@code null}.
   */
  EntryWriter writeEntry(Entry entry) throws IOException,
      NullPointerException;

}
