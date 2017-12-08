import grpc
import time

from concurrent import futures
import pipeline_pb2
import pipeline_pb2_grpc
import argparse
import sys

_ONE_DAY_IN_SECONDS = 60 * 60 * 24
DEFAULT_PORT = 50051

class GatewayServicer(pipeline_pb2_grpc.gatewayServicer):

    def forward_data(self, data):
        channel = grpc.insecure_channel('%s:%s' % (host_next, port_next))
        stub = pipeline_pb2_grpc.gatewayStub(channel)
        print ("----push_data----")
        grid_data_list = [data[0],data[1]]
        data_iterator = generate_iterator(grid_data_list)
        data_push_response = stub.push_data(data_iterator)
        print ("----data pushed----")
        print ("Response Code: %s " % data_push_response.response_code)

    def push_data(self, request_iterator, context):
        key = 0
        data = {}
        for grid_data in request_iterator:

            data[key]= grid_data
            key+=1
        # missing: data forwarding to next hop so just print
        if gateway_bool == True:
            self.forward_data(data)
        print data
        return pipeline_pb2.reply(response_code="200")

def prep_argparse():
    parser = argparse.ArgumentParser(description='Server to serve RPC calls for gateway')
    parser.add_argument('serve', help='start server')

    parser.add_argument('-p', '--port', help="port for this component (only gateway, endpoint)")
    parser.add_argument('-pn', '--port_next', help="port for the next_hop (only for role gateway and source)", default=50051)
    parser.add_argument('-hn', '--host_next', help="host for the next hop (only for role gateway and source)", default="localhost")
    parser.add_argument('-r', '--role', help="role in pipeline {source, gateway, endpoint} ", default="source")
    # parser.add_argument('-st', '--storagetime', help="specify the storage time of messages (only for gateways and endpoints)")
    parser.add_argument('-i', '--interval', help="interval (seconds) a message will be generated and sent (only for role source)", default=1)
    parser.add_argument('-n', '--number_msg', help="number of messages that will be sent (only for role source)")
    return parser

###### code for a data source

def make_grid_data_message(meterid,metricid,timestamp,value):
    return pipeline_pb2.Grid_data(
        measurement = pipeline_pb2.measurement_message(meter_id=meterid,metric_id=metricid, timestamp=timestamp, value=value))

def generate_iterator(grid_data_list):
    for i in grid_data_list:
        yield i

def gateway_push_data(stub):
    grid_data_list = [make_grid_data_message("1","2",1645,99),make_grid_data_message("2","2",1646,90)]

    data_iterator = generate_iterator(grid_data_list)
    data_push_response = stub.push_data(data_iterator)
    print ("data pushed")
    print ("Response Code: %s " % data_push_response.response_code)

def run_as_source(args):
    port = int(args.port_next) if args.port_next is not None else DEFAULT_PORT
    interval = int(args.interval)
    numberOfMsgs = int(args.number_msg) if args.number_msg is not None else -1
    receiver = args.host_next if args.host_next is not None else "localhost"
    
    channel = grpc.insecure_channel('%s:%s' % (receiver, port))
    stub = pipeline_pb2_grpc.gatewayStub(channel)
    print ("----push_data----")       

    sentMsgs = 0
    gateway_push_data(stub)

    while (sentMsgs != numberOfMsgs):
        gateway_push_data(stub)
        sentMsgs += 1
        time.sleep(interval)

    print (" FINISHED. Sent messages: %d " % sentMsgs);
      


###### gateway code
def run_as_gateway(args):
    ## parse host/port for the next hop
    global gateway_bool;
    gateway_bool = True
    
    global port_next
    global host_next

    port_next = int(args.port_next) if args.port_next is not None else DEFAULT_PORT
    host_next = args.host_next if args.host_next is not None else "localhost"
    port = int(args.port) if args.port is not None else DEFAULT_PORT
    
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pipeline_pb2_grpc.add_gatewayServicer_to_server(GatewayServicer(), server)
    
    ## channel_in
    server.add_insecure_port('[::]:%s' % port)
    server.start()
    try:
        while True:
            time.sleep(_ONE_DAY_IN_SECONDS)
    except KeyboardInterrupt:
        server.stop(0)

# endpoint code
def run_as_endpoint(args):
    global gateway_bool
    gateway_bool = False;
    
    port = int(args.port) if args.port is not None else DEFAULT_PORT    
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pipeline_pb2_grpc.add_gatewayServicer_to_server(GatewayServicer(), server)
    server.add_insecure_port('[::]:%s' % port)
    server.start()
    try:
        while True:
            time.sleep(_ONE_DAY_IN_SECONDS)
    except KeyboardInterrupt:
        server.stop(0)


def main(argv):
    ## example for local deployment
    #   data source:
    #   python pipeline_component.py -r source -pn 50052
    #
    #   gateway:
    #   python pipeline_component.py -r gateway -p 50052 -pn 50053
    #
    #   endpoint:
    #   python pipeline_component.py -r endpoint -p 50053

    parser = prep_argparse()
    args = parser.parse_args(argv)

    print(args)

    if(args.role == "source"):
        print("run as data source")
        run_as_source(args)
    elif(args.role == "gateway"):
        print("run as gateway")
        run_as_gateway(args)
    elif(args.role == "endpoint"):
        print("run as endpoint")
        run_as_endpoint(args)


if __name__ == '__main__':
    main(sys.argv)
