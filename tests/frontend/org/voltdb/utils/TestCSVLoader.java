package org.voltdb.utils;

import java.io.FileReader;

import org.voltdb.ServerThread;
import org.voltdb.VoltDB;
import org.voltdb.VoltTable;
import org.voltdb.VoltDB.Configuration;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.compiler.VoltProjectBuilder;

import au.com.bytecode.opencsv_voltpatches.CSVReader;

import junit.framework.TestCase;

public class TestCSVLoader extends TestCase {
	public void testSimple() throws Exception {
        String simpleSchema =
            "create table BLAH (" +
            "clm_tinyint tinyint default null, " +
            "clm_smallint smallint default null, " +
            "clm_integer integer default 0 not null, " +
            "clm_bigint bigint default null, " +
            
            "clm_float float default null, " +
            "clm_timestamp timestamp default null, " +
            "clm_string varchar(10) default null, " +
            "clm_decimal decimal default null, " +
            "clm_varinary varbinary default null); ";
            

        String pathToCatalog = Configuration.getPathToCatalogForTest("csv.jar");
        String pathToDeployment = Configuration.getPathToCatalogForTest("csv.xml");

        VoltProjectBuilder builder = new VoltProjectBuilder();
        builder.addLiteralSchema(simpleSchema);
        builder.addPartitionInfo("BLAH", "clm_integer");
        //builder.addStmtProcedure("Insert", "insert into blah values (?, ?, ?);", null);
        //builder.addStmtProcedure("InsertWithDate", "INSERT INTO BLAH VALUES (974599638818488300, 5, 'nullchar');");
        boolean success = builder.compile(pathToCatalog, 2, 1, 0);
        assertTrue(success);
        MiscUtils.copyFile(builder.getPathToDeployment(), pathToDeployment);

        VoltDB.Configuration config = new VoltDB.Configuration();
        config.m_pathToCatalog = pathToCatalog;
        config.m_pathToDeployment = pathToDeployment;
        ServerThread localServer = new ServerThread(config);

        Client client = null;
        try {
            localServer.start();
            localServer.waitForInitialization();

            String []params = {"/Users/xinjia/testdb.csv","Insert","--columns", "0,2,1"};
            //String []params = {"/Users/xinjia/testdb.csv","Insert","--columns", "0,2,1"};
            CSVLoader.main(params);
            
            // do the test
            client = ClientFactory.createClient();
            client.createConnection("localhost");
            
            final CSVReader reader = new CSVReader(new FileReader(params[0]));
            int lineCount = 0;
            while (reader.readNext() != null) {
            	lineCount++;
            }
            
            VoltTable modCount;
            modCount = client.callProcedure("@AdHoc", "SELECT COUNT(*) FROM BLAH;").getResults()[0];
            int rowct = 0;
            while(modCount.advanceRow()) {
            	rowct = (Integer) modCount.get(0, VoltType.INTEGER);
            }
            System.out.println(String.format("The rows infected: (%d,%s)", lineCount, rowct));
            assertEquals(lineCount, rowct);
            
            modCount = client.callProcedure("@AdHoc", "SELECT * FROM BLAH;").getResults()[0];
            System.out.println("data inserted to table BLAH:\n" + modCount);
            
        }
        finally {
            if (client != null) client.close();
            client = null;

            if (localServer != null) {
                localServer.shutdown();
                localServer.join();
            }
            localServer = null;

            // no clue how helpful this is
            System.gc();
        }
        
        
	}
	
	
	
	
	
}
