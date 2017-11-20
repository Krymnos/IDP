import grpc
import time

from concurrent import futures
import pipeline_pb2
import pipeline_pb2_grpc
import argparse
import sys
import gateway_client as gc

_ONE_DAY_IN_SECONDS = 60 * 60 * 24
DEFAULT_PORT = 50051




class GatewayServicer(pipeline_pb2_grpc.gatewayServicer):

    def forward_data(self, data):
        channel = grpc.insecure_channel('localhost:%s' % port_next)
        stub = pipeline_pb2_grpc.gatewayStub(channel)
        print ("----push_data----")
        grid_data_list = [data[0],data[1]]
        data_iterator = gc.generate_iterator(grid_data_list)
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



def main(argv):
    parser = prep_argparse()
    args = parser.parse_args(argv)
    server = serve(args)


def prep_argparse():
    parser = argparse.ArgumentParser(description='Server to serve RPC calls for gateway')
    parser.add_argument('serve', help='start server')
    parser.add_argument('-p', '--port', help="Default port for the server")
    parser.add_argument('-g', '--gateway', help="setup server as gateway", action="store_true")
    parser.add_argument('-pn', '--port_next', help="port for the next_hop", default=None)
    return parser

def serve(args):
    port = int(args.port) if args.port is not None else DEFAULT_PORT
    if args.gateway == True :
        global gateway_bool
        gateway_bool = True
    else:
        global gateway_bool
        gateway_bool = False

    print gateway_bool
    global port_next
    port_next = int(args.port_next) if args.port_next is not None else None

    print (port)
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pipeline_pb2_grpc.add_gatewayServicer_to_server(GatewayServicer(), server)
    server.add_insecure_port('[::]:%s' % port)
    server.start()
    try:
        while True:
            time.sleep(_ONE_DAY_IN_SECONDS)
    except KeyboardInterrupt:
        server.stop(0)

if __name__ == '__main__':
    print ("Example usage:")
    print ("python gateway_server.py  -g -p \"50051\" -pn \"50055\" ")
    main(sys.argv)