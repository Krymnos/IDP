FROM python:2.7-slim

COPY [gateway_server.py,  pipeline_*] . /

RUN pip install protobuf grpc grpcio-tools grpcio

CMD [ "./simple_pipeline.sh" ]
