package com.swijaya.portlistener;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class App  {

    static {
        // configure the logging backend for netty's internal logger (used in LoggingHandler)
        InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
    }

    public static final Logger LOGGER = LogManager.getLogger(App.class.getName());

    private PortsConfig portsConfig;
    private boolean useIpv4, useIpv6;
    private boolean useTcp, useUdp;
    private boolean verbose;

    private LinkedList<ChannelFactory> factories;
    private LinkedList<Bootstrap> bootstraps;

    private final ChannelGroup allChannels = new DefaultChannelGroup("portlistener");

    public App(
            PortsConfig portsConfig,
            boolean useIpv4,
            boolean useIpv6,
            boolean useTcp,
            boolean useUdp,
            boolean verbose) {
        this.portsConfig = portsConfig;
        this.useIpv4 = useIpv4;
        this.useIpv6 = useIpv6;
        this.useTcp = useTcp;
        this.useUdp = useUdp;
        this.verbose = verbose;
    }

    void configure() {
        factories = new LinkedList<ChannelFactory>();
        bootstraps = new LinkedList<Bootstrap>();

        // configure the pipeline where our DISCARD logic lies
        ChannelPipeline p = pipeline();
        p.addLast("discard", new DiscardServerHandler(verbose, allChannels));
        if (verbose)
            p.addLast("log", new LoggingHandler("PortListener"));

        // configure a stream server bootstrap (if configured)
        if (useTcp) {
            LOGGER.trace("Configuring a stream server socket factory");
            ChannelFactory f = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool());
            Bootstrap b = new ServerBootstrap(f);
            b.setPipeline(p);

            // further configure connected sockets that would be created for clients of this acceptor
            b.setOption("child.tcpNoDelay", true);
            b.setOption("child.keepAlive", true);

            factories.add(f);
            bootstraps.add(b);
        }

        // configure a datagram server bootstrap (if configured)
        if (useUdp) {
            LOGGER.trace("Configuring a datagram channel factory");
            ChannelFactory f = new NioDatagramChannelFactory(
                    Executors.newCachedThreadPool());
            Bootstrap b = new ConnectionlessBootstrap(f);
            b.setPipeline(p);

            // configure server sockets' options
            // TODO

            factories.add(f);
            bootstraps.add(b);
        }
    }

    void bind() {
        if (useIpv4)
            LOGGER.trace("Using IPv4");
        else if (useIpv6)
            LOGGER.trace("Using IPv6");

        // bind our server bootstraps to configured ports
        for (Bootstrap b : bootstraps) {
            for (Integer port : portsConfig) {
                Channel ch;
                InetSocketAddress addr;

                if (useIpv4 && useIpv6)
                    addr = new InetSocketAddress(port);
                else if (useIpv4)
                    addr = new InetSocketAddress("0.0.0.0", port);
                else
                    addr = new InetSocketAddress("::", port);

                if (b instanceof ServerBootstrap) {
                    LOGGER.trace("Binding a server bootstrap to " + addr.toString());
                    ch = ((ServerBootstrap) b).bind(addr);
                }
                else if (b instanceof ConnectionlessBootstrap) {
                    LOGGER.trace("Binding a datagram bootstrap to " + addr.toString());
                    ch = ((ConnectionlessBootstrap) b).bind(addr);
                }
                else {
                    throw new IllegalStateException("Illegal bootstrap object");
                }
                allChannels.add(ch);
            }
        }
    }

    public void run() throws Exception {
        configure();

        bind();

        // handle Ctrl-C to clean up after ourselves
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.trace("Exiting application.");
                ChannelGroupFuture f = allChannels.close();
                f.awaitUninterruptibly();
                for (ChannelFactory factory : factories)
                    factory.releaseExternalResources();
                LOGGER.debug("Shutdown.");
            }
        });
    }

    @SuppressWarnings("static-access")
    public static void main( String[] args ) throws Exception {
        LOGGER.trace("Entering application.");

        // build the command-line options declaration
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("v", "verbose", false, "be extra verbose");
        options.addOption("u", false, "open datagram listener(s) (default)");
        options.addOption("t", false, "open stream listener(s) (default)");
        options.addOption("4", false, "bind listener(s) on IPv4 address(es) (default)");
        options.addOption("6", false, "bind listener(s) on IPv6 address(es) (default)");
        options.addOption(OptionBuilder.withLongOpt("ports")
                .withDescription("bind listener(s) on this set of ports "
                        + "and port ranges, separated with commas "
                        + "(e.g. 8080,5550-5555,6767) (required)")
                .hasArg()
                .isRequired()
                .withArgName("PORTS")
                .create("p"));

        HelpFormatter formatter = new HelpFormatter();

        // parse command line arguments
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
            formatter.printHelp("portlistener", options);
            return;
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("portlistener", options);
            return;
        }

        boolean verbose = cmd.hasOption("v");
        boolean useUdp = cmd.hasOption("u");
        boolean useTcp = cmd.hasOption("t");
        boolean useIpv4 = cmd.hasOption("4");
        boolean useIpv6 = cmd.hasOption("6");

        // normalize options with defaults
        if (!useUdp && !useTcp) {
            // by default, if none of the connection type is configured, we assume both
            useUdp = useTcp = true;
        }
        if (!useIpv4 && !useIpv6) {
            // by default, if none of the network type is configured, we assume both
            useIpv4 = useIpv6 = true;
        }

        // parse ports configuration
        String portsOption = cmd.getOptionValue("p");
        PortsConfig portsConfig = new PortsConfig();
        String[] tokens = portsOption.split(",");
        try {
            for (String token : tokens) {
                String[] ranges = token.split("-");
                switch (ranges.length) {
                    case 1:
                        // a single port
                        try {
                            portsConfig.addPort(Integer.parseInt(ranges[0]));
                        } catch (NumberFormatException e) {
                            throw new ParseException("Invalid port number in token: " + token);
                        } catch (IllegalArgumentException e) {
                            throw new ParseException("Invalid port number in token: " + token);
                        }
                        break;
                    case 2:
                        // a start-end port range
                        try {
                            portsConfig.addPortRange(Integer.parseInt(ranges[0]), Integer.parseInt(ranges[1]));
                        } catch (NumberFormatException e) {
                            throw new ParseException("Invalid port number in token: " + token);
                        } catch (IllegalArgumentException e) {
                            throw new ParseException("Invalid port number in token: " + token);
                        }
                        break;
                    default:
                        throw new ParseException("Invalid port(s) specification in token: " + token);
                }
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("portlistener", options);
            return;
        }

        new App(
                portsConfig,
                useIpv4,
                useIpv6,
                useTcp,
                useUdp,
                verbose)
                .run();
    }

    private static class PortsConfig implements Iterable<Integer> {

        public LinkedList<Integer> ports = new LinkedList<Integer>();

        public void addPort(int port) {
            if (port <= 0 || port > 65535)
                throw new IllegalArgumentException("Invalid port number: " + Integer.toString(port));
            ports.add(port);
        }

        public void addPortRange(int start, int end) {
            if (start <= 0 || start > 65535)
                throw new IllegalArgumentException("Invalid port number: " + Integer.toString(start));
            if (end <= 0 || end > 65535)
                throw new IllegalArgumentException("Invalid port number: " + Integer.toString(end));
            if (end < start)
                throw new IllegalArgumentException("Invalid port range: " + Integer.toString(start) + "-" + Integer.toString(end));
            for (int i = start; i <= end; i++) {
                ports.add(i);
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return ports.iterator();
        }
    }

}
