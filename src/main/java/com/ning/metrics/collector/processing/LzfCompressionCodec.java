/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.collector.processing;

import com.ning.compress.lzf.util.LZFFileOutputStream;
import com.ning.metrics.serialization.writer.CompressionCodec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class LzfCompressionCodec implements CompressionCodec
{
    @Override
    public FileOutputStream getFileOutputStream(final File file) throws FileNotFoundException
    {
        return new LZFFileOutputStream(file);
    }
}
