/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.source;


public class ExecTailSourceConfigurationConstants {

  /**
   * Should the exec'ed command restarted if it dies: : default false
   */
  public static final String CONFIG_RESTART = "restart";
  public static final boolean DEFAULT_RESTART = false;
  public static final boolean DEFAULT_RESTART_TRUE = true;
  public static final boolean DEFAULT_STARTATBEGINNING = false;

  public static final boolean DEFAULT_ISTAILING_TRUE = true;
  public static final boolean DEFAULT_CONTEXTISJSON = false;
  public static final String DEFAULT_MSGTYPECONFIG = "";

  /**
   * config rule for get attributes from string
   */
  public static final String DEFAULT_MSGTYPECONFIG_DEFULT = "defult:className|methodName|level|treeId|requestId|transactionId,PerformanceFile:logType|className|methodName|level|treeId|requestId|transactionId|isBeginMethod|methodStart|methodEnd|spendTime|methodParameter|returnValue";


  /**
   * config default reader like no es rule
   */
  public static final boolean DEFAULT_CONTEXTISFLUMELOG = false;

  /**
   * config default file for write line size for read from this line when program is dead;
   */
  public static final String DEFAULT_FILEWRITEJSON = "/data/flume/fileJson.json";


  public static final String DEFAULT_DOMAIN = null;
  public static final Long DEFAULT_FLUSHTIME = 5000L;

  public static final Integer DEFAULT_READINTERVAL = 500;

  /**
   * Amount of time to wait before attempting a restart: : default 10000 ms
   */
  public static final String CONFIG_RESTART_THROTTLE = "restartThrottle";
  public static final long DEFAULT_RESTART_THROTTLE = 10000L;

  /**
   * config this source is working or sleep
   */
  public static final String CONFIG_TAILING_THROTTLE = "tailing";

  /**
   * config yourself rule for build json attributes
   */
  public static final String CONFIG_MSGTYPECONFIG_THROTTLE = "msgTypeConfig";

  /**
   * config your string is json
   */
  public static final String CONFIG_CONTEXTISJSON_THROTTLE = "contextIsJson";

  /**
   * config you don't use CONFIG_MSGTYPECONFIG_THROTTLE
   */
  public static final String CONFIG_CONTEXTISFLUMELOG_THROTTLE = "contextIsFlumeLog";

  /**
   * change default file for write line size for read from this line when program is dead;
   */
  public static final String CONFIG_FILEWRITEJSON_THROTTLE = "fileWriteJson";

  /**
   * make your domain name
   */
  public static final String CONFIG_DOMIAN_THROTTLE = "domain";
  public static final String CONFIG_FLUSHTIME_THROTTLE = "flushTime";
  public static final String CONFIG_READINTERVAL_THROTTLE = "readinterval";

  /**
   * config read file from file's top
   */
  public static final String CONFIG_STARTATBEGINNING_THROTTLE = "startAtBeginning";

  /**
   * Should stderr from the command be logged: default false
   */
  public static final String CONFIG_LOG_STDERR = "logStdErr";
  public static final boolean DEFAULT_LOG_STDERR = false;

  /**
   * Number of lines to read at a time
   */
  public static final String CONFIG_BATCH_SIZE = "batchSize";
  public static final int DEFAULT_BATCH_SIZE = 20;

  /**
   * Amount of time to wait, if the buffer size was not reached, before 
   * to data is pushed downstream: : default 3000 ms
   */
  public static final String CONFIG_BATCH_TIME_OUT = "batchTimeout";
  public static final long DEFAULT_BATCH_TIME_OUT = 3000l;

  /**
   * Charset for reading input
   */
  public static final String CHARSET = "charset";
  public static final String DEFAULT_CHARSET = "UTF-8";

  /**
   * Optional shell/command processor used to run command
   */
  public static final String CONFIG_SHELL = "shell";

  /**
   * RANDOMACCESSFILE read file defalut charset is 8859_1
   */
  public static final String CHARSET_RANDOMACCESSFILE = "ISO-8859-1";

  /**
   * when readLine make same attributes as int type
   */
  public static final String MAP_INT_ATTRIBUTE="created,methodStart,methodEnd,spendTime";
}
