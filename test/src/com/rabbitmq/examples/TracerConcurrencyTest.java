//  The contents of this file are subject to the Mozilla Public License
//  Version 1.1 (the "License"); you may not use this file except in
//  compliance with the License. You may obtain a copy of the License
//  at http://www.mozilla.org/MPL/
//
//  Software distributed under the License is distributed on an "AS IS"
//  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
//  the License for the specific language governing rights and
//  limitations under the License.
//
//  The Original Code is RabbitMQ.
//
//  The Initial Developer of the Original Code is VMware, Inc.
//  Copyright (c) 2007-2011 VMware, Inc.  All rights reserved.
//

package com.rabbitmq.examples;

import com.rabbitmq.client.*;
import java.util.concurrent.*;
import java.util.Random;

/** 
 * Test that the tracer correctly handles multiple concurrently processing
 * channels. If it doesn't then this code will cause the tracer to break
 * and will be disconneted.
 */
public class TracerConcurrencyTest{

  public int port = 5673;
  public String host = "localhost";
  public int threadCount = 3;

  private final Object lock = new Object();

  public static void main(String[] args) throws Exception{
    new TracerConcurrencyTest().run();
  }

  static String EXCHANGE = "tracer-exchange";
  static String QUEUE = "tracer-queue";

  public void run(){

    final Connection conn;
    try {
      conn = new ConnectionFactory()
          {{setHost(host); setPort(port);}}.newConnection();
      Channel setup = conn.createChannel();

      setup.exchangeDeclare(EXCHANGE, "direct");
      setup.queueDeclare(QUEUE, false, false, false, null);
      setup.queueBind(QUEUE,EXCHANGE, "");
      
      setup.close();
    } catch(Exception e){
      e.printStackTrace();
      System.exit(1);
      throw null; // placate the compiler
    }

    for(int i = 0; i < threadCount; i++){
      final int j = i;
      new Thread(){
        @Override public void run(){
          try {
            Random rnd = new Random();
            Channel ch = conn.createChannel();
            while(true){
                Channel old = ch;
                ch.close();
                ch = conn.createChannel(); 
                ch.basicPublish(
                  EXCHANGE,
                  "", null,
                  new byte[1024 * 1024]
                );
                ch.basicGet(QUEUE, true);
            }
          } catch(Exception e){
            synchronized(lock){
              e.printStackTrace();
              System.err.println();
            }
            System.exit(1);
          } 
        }
      }.start();
    }
  }
}
