import grpc
import time

from concurrent import futures
import pipeline_pb2
import pipeline_pb2_grpc


_ONE_DAY_IN_SECONDS = 60 * 60 * 24

class GatewayServicer(pipeline_pb2_grpc.gatewayServicer):

    def push_data(self, request_iterator, context):
        key = 0
        data = {}
        for grid_data in request_iterator:

            data[key]= grid_data
            key+=1
        # missing: data forwarding to next hop so just print
        print data
        return pipeline_pb2.reply(response_code="200")


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pipeline_pb2_grpc.add_gatewayServicer_to_server(GatewayServicer(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    try:
        while True:
            time.sleep(_ONE_DAY_IN_SECONDS)
    except KeyboardInterrupt:
        server.stop(0)

if __name__ == '__main__':
    serve()