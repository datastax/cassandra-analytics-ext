/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.spark.example;

public final class Launcher
{
    private Launcher()
    {
        throw new IllegalStateException(getClass() + " is static class and shall not be instantiated");
    }

    public static void main(String[] args)
    {
        String job = args[0];

        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);

        if ("write".equalsIgnoreCase(job))
        {
            DataImportJob.start(newArgs);
        }
        else if ("count".equalsIgnoreCase(job))
        {
            RowCountJob.start(newArgs);
        }
        else
        {
            System.out.println("Choose 'count' or 'write' job.");
        }
    }
}
