# flume-files-source
Collect data from multiple files, and support for HTTP.（从多个文件收集数据，并支持断点续传。）
 
step：
  1,config one path
  2,find all file with RegExp
  3,tail one children file
  4,batch to channal
  5,属性contextIsFlumeLog 如果等于true，数据必须以'\'分割，如要自定义数据格式，请在msgTypeConfig属性中添加，例如
    agent1.sources.source1.msgTypeConfig=one_type:className|methodName|level|treeId|requestId|transactionId
     If the contextIsFlumeLog attribute is equal to true,
        the data must be | segmentation,
        such as custom data format, please add in the msgTypeConfig attribute, for example:
        agent1.sources.source1.msgTypeConfig=one_type:className|methodName|level|treeId|requestId|transactionId
  6,如果数据是json格式的，请配置属性contextIsJson为true，则数据不会进行任何处理，直接传输到channel
        If the data is JSON format, please configure the property true for contextIsJson, then the data will not be processed directly to the channel
 
 demo 1 ：File format special, '|' to split data:
  agent1.sources.source1.filepath=/export/home/tomcat/logs/tender.soa.el.com/apilogs/
  agent1.sources.source1.filenameRegExp=(.log{1})$
  agent1.sources.source1.readinterval=300
  agent1.sources.source1.startAtBeginning=false
  agent1.sources.source1.restart=true
  agent1.sources.source1.tailing=true
  agent1.sources.source1.contextIsJson=false
  agent1.sources.source1.contextIsFlumeLog=true
  agent1.sources.source1.domain=tender.soa.el.com
  agent1.sources.source1.fileWriteJson=/export/home/flume_elo/filelogs/fileJson_account.json
 
作者：xvshu
时间：2016-8-5
版本：1.3.0
 
