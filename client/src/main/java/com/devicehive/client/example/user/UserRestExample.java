package com.devicehive.client.example.user;


import com.devicehive.client.model.Transport;

import java.io.PrintStream;
import java.net.URI;

public class UserRestExample extends UserExample {

    public UserRestExample(PrintStream out) {
        super(out);
    }

    /**
     * example's main method
     *
     * @param args args[0] - REST server URI
     */
    public static void main(String... args) {
        UserRestExample example = new UserRestExample(System.out);
        if (args.length < 1) {
            example.printUsage();
        } else {
            URI rest = URI.create(args[0]);
            example.run(rest, Transport.REST_ONLY);
        }
    }


}