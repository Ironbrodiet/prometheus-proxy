# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import google.protobuf.empty_pb2 as google_dot_protobuf_dot_empty__pb2
import grpc

import proxy_service_pb2 as proxy__service__pb2


class ProxyServiceStub(object):
    def __init__(self, channel):
        """Constructor.
    
        Args:
          channel: A grpc.Channel.
        """
        self.connectAgent = channel.unary_unary(
            '/proxy_service.ProxyService/connectAgent',
            request_serializer=google_dot_protobuf_dot_empty__pb2.Empty.SerializeToString,
            response_deserializer=google_dot_protobuf_dot_empty__pb2.Empty.FromString,
        )
        self.registerAgent = channel.unary_unary(
            '/proxy_service.ProxyService/registerAgent',
            request_serializer=proxy__service__pb2.RegisterAgentRequest.SerializeToString,
            response_deserializer=proxy__service__pb2.RegisterAgentResponse.FromString,
        )
        self.registerPath = channel.unary_unary(
            '/proxy_service.ProxyService/registerPath',
            request_serializer=proxy__service__pb2.RegisterPathRequest.SerializeToString,
            response_deserializer=proxy__service__pb2.RegisterPathResponse.FromString,
        )
        self.readRequestsFromProxy = channel.unary_stream(
            '/proxy_service.ProxyService/readRequestsFromProxy',
            request_serializer=proxy__service__pb2.AgentInfo.SerializeToString,
            response_deserializer=proxy__service__pb2.ScrapeRequest.FromString,
        )
        self.writeResponseToProxy = channel.unary_unary(
            '/proxy_service.ProxyService/writeResponseToProxy',
            request_serializer=proxy__service__pb2.ScrapeResponse.SerializeToString,
            response_deserializer=google_dot_protobuf_dot_empty__pb2.Empty.FromString,
        )


class ProxyServiceServicer(object):
    def connectAgent(self, request, context):
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def registerAgent(self, request, context):
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def registerPath(self, request, context):
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def readRequestsFromProxy(self, request, context):
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def writeResponseToProxy(self, request, context):
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_ProxyServiceServicer_to_server(servicer, server):
    rpc_method_handlers = {
        'connectAgent': grpc.unary_unary_rpc_method_handler(
            servicer.connectAgent,
            request_deserializer=google_dot_protobuf_dot_empty__pb2.Empty.FromString,
            response_serializer=google_dot_protobuf_dot_empty__pb2.Empty.SerializeToString,
        ),
        'registerAgent': grpc.unary_unary_rpc_method_handler(
            servicer.registerAgent,
            request_deserializer=proxy__service__pb2.RegisterAgentRequest.FromString,
            response_serializer=proxy__service__pb2.RegisterAgentResponse.SerializeToString,
        ),
        'registerPath': grpc.unary_unary_rpc_method_handler(
            servicer.registerPath,
            request_deserializer=proxy__service__pb2.RegisterPathRequest.FromString,
            response_serializer=proxy__service__pb2.RegisterPathResponse.SerializeToString,
        ),
        'readRequestsFromProxy': grpc.unary_stream_rpc_method_handler(
            servicer.readRequestsFromProxy,
            request_deserializer=proxy__service__pb2.AgentInfo.FromString,
            response_serializer=proxy__service__pb2.ScrapeRequest.SerializeToString,
        ),
        'writeResponseToProxy': grpc.unary_unary_rpc_method_handler(
            servicer.writeResponseToProxy,
            request_deserializer=proxy__service__pb2.ScrapeResponse.FromString,
            response_serializer=google_dot_protobuf_dot_empty__pb2.Empty.SerializeToString,
        ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
        'proxy_service.ProxyService', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))