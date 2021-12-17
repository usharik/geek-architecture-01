package ru.geekbrains.handler;

import ru.geekbrains.config.Config;
import ru.geekbrains.domain.HttpRequest;
import ru.geekbrains.domain.HttpResponse;
import ru.geekbrains.domain.ResponseCode;
import ru.geekbrains.service.RequestParser;
import ru.geekbrains.service.ResponseSerializer;
import ru.geekbrains.service.SocketService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;

public class RequestHandler implements Runnable {

    private final SocketService socketService;
    private final RequestParser requestParser;
    private final ResponseSerializer responseSerializer;
    private final Config config;

    public RequestHandler(SocketService socketService,
                          RequestParser requestParser,
                          ResponseSerializer responseSerializer,
                          Config config) {
        this.socketService = socketService;
        this.requestParser = requestParser;
        this.responseSerializer = responseSerializer;
        this.config = config;
    }

    @Override
    public void run() {
        Deque<String> rawRequest = socketService.readRequest();
        HttpRequest httpRequest = requestParser.parseRequest(rawRequest);

        if (httpRequest.getMethod().equals("GET")) {
            Path path = Paths.get(config.getWwwHome(), httpRequest.getUrl());

            if (!Files.exists(path)) {
                HttpResponse response = HttpResponse.createBuilder()
                        .withStatus(ResponseCode.NOT_FOUND)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody("<h1>Файл не найден!</h1>")
                        .build();

                String rawResponse = responseSerializer.serialize(response);
                socketService.writeResponse(rawResponse);
                return;
            }

            StringBuilder sb = new StringBuilder();
            try {
                Files.readAllLines(path).forEach(sb::append);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            HttpResponse response = HttpResponse.createBuilder()
                    .withStatus(ResponseCode.OK)
                    .withHeader("Content-Type", "text/html; charset=utf-8")
                    .withBody(sb.toString())
                    .build();

            String rawResponse = responseSerializer.serialize(response);
            socketService.writeResponse(rawResponse);
        } else {
            HttpResponse response = HttpResponse.createBuilder()
                    .withStatus(ResponseCode.METHOD_NOT_ALLOWED)
                    .withHeader("Content-Type", "text/html; charset=utf-8")
                    .withBody("<h1>Метод не поддерживается!</h1>")
                    .build();

            String rawResponse = responseSerializer.serialize(response);
            socketService.writeResponse(rawResponse);
            return;
        }
        try {
            socketService.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("Client disconnected!");
    }
}
