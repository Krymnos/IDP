package de.idp.pipeline;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	RedisClient dbClient;
    	StatefulRedisConnection<String, String> dbConnection;
    	dbClient = RedisClient.create("redis://localhost:6379");
    	dbConnection = dbClient.connect();
    	RedisCommands<String, String> commands;
    	commands = dbConnection.sync();
        commands.flushdb();
        System.out.println("Hallo");
        assertTrue( true );
    }
}
