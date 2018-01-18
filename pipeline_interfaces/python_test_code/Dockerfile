FROM python:2.7-slim

COPY simple_pipeline.sh ./
COPY pipeline_*.py ./

RUN pip install protobuf grpc grpcio-tools grpcio

CMD [ "./simple_pipeline.sh" ]
