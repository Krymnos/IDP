package de.idp.pipeline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class PipelineComponent {

    public static void main(String[] argv){

        Args args = new Args();
        JCommander comm = JCommander.newBuilder()
                .addObject(args)
                .build();

        comm.parse(argv);

        if(args.help){
          comm.usage();
          return;
        }

        System.out.println(args.toString());
    }

}

class Args {
    @Parameter(names = { "-p", "--port" }, description = "port for this component")
    public Integer port;

    @Parameter(names = { "-pn", "--port_next" }, description = "port for the next_hop")
    public Integer port_next;

    @Parameter(names = { "-hn", "--host_next" }, description = "host for the next hop. If this is not set then this"
                                                             + " node is an endpoint.")
    public String host_next;

    @Parameter(names = {"-agg", "--aggregate"}, description = "aggregate message values (min, max, avg) of the last n"
                                                            + " seconds. If this is not set then no aggregation takes place.")
    public Integer aggregationTime;

    @Parameter(names = {"-st", "--storagetime"}, description = "specify the storagetime of messages (in seconds)")
    public Integer storageTime;

    @Parameter(names = "--help", description = "prints this help", help = true)
    public boolean help;

    @Override public String toString() {
      return "Args{" +
              "port=" + port +
              ", port_next=" + port_next +
              ", host_next='" + host_next + '\'' +
              ", aggregationTime=" + aggregationTime +
              ", storageTime=" + storageTime +
              '}';
    }
}