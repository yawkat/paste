package at.yawk.paste.server;

import at.yawk.yarn.ComponentScan;
import at.yawk.yarn.EntryPoint;
import at.yawk.yarn.Yarn;

/**
 * @author yawkat
 */
@EntryPoint
@ComponentScan
public abstract class Server {
    protected Server() {}

    public static void main(String[] args) {
        Yarn.build(Server.class);
    }
}
