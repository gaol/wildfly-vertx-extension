/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.wildfly.extension.vertx.test.shared;

import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Utils class on stream string conversion.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class StreamUtils {

    /**
     * Reads InputStream and converts to a String using UTF-8 encoding.
     *  This won't close the stream.
     *
     * @param input the input stream
     * @return the String representation of the content reads from the stream.
     * @throws IOException Exception that may occur
     */
    public static String streamToString(InputStream input) throws IOException {
        return streamToString(input, false);
    }

    /**
     * Reads InputStream and converts to a String using UTF-8 encoding.
     *
     * @param input the input stream
     * @param closeStream if the stream should be closed at the end
     * @return the String representation of the content reads from the stream.
     * @throws IOException Exception that may occur
     */
    public static String streamToString(InputStream input, boolean closeStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        try {
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (closeStream) {
                reader.close();
                input.close();
            }
        }
        return sb.toString();
    }

    /**
     * Converts the content read from the stream to a JsonObject.
     *
     * @param input the input stream
     * @param closeStream if the stream should be closed at the end
     * @return a JsonObject instance with the String representation of the content reads from the stream.
     * @throws IOException Exception that may occur
     */
    public static JsonObject streamToJsonObject(InputStream input, boolean closeStream) throws IOException {
        return new JsonObject(streamToString(input, closeStream));
    }

    /**
     * Converts the content read from the stream to a Properties.
     *
     * @param input the input stream
     * @param closeStream if the stream should be closed at the end
     * @return a Properties instance with the String representation of the content reads from the stream.
     * @throws IOException Exception that may occur
     */
    public static Properties streamToProperties(InputStream input, boolean closeStream) throws IOException {
        Properties props = new Properties();
        try {
            props.load(input);
        } finally {
            if (closeStream) {
                input.close();
            }
        }
        return props;
    }

    /**
     * Converts String representation to a Properties instance.
     *
     * @param data string content that can be converted to a Properties
     * @return a Properties instance
     */
    public static Properties stringToProperties(String data)  {
        Properties props = new Properties();
        try (StringReader sr = new StringReader(data)) {
            props.load(sr);
        } catch (IOException ioe) {
            // ignore
        }
        return props;
    }
}
