/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.server2;

import static java.util.logging.Level.INFO;

import java.io.File;
import java.io.IOException;
import java.security.Policy;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import cloud.piranha.api.Piranha;
import cloud.piranha.appserver.impl.DefaultWebApplicationServer;
import cloud.piranha.http.impl.DefaultHttpServer;
import cloud.piranha.micro.MicroConfiguration;
import cloud.piranha.micro.MicroOuterDeployer;

/**
 * The Servlet container version of Piranha.
 *
 * <p>
 * This version of Piranha makes it possible for you to run multiple web
 * applications at the same time.
 * </p>
 *
 * <p>
 * It has a shutdown mechanism that allows you to shutdown the server by
 * removing the piranha.pid file that should be created by the startup script.
 * </p>
 *
 * @author Manfred Riem (mriem@manorrock.com)
 * @author Arjan Tijms
 */
public class ServerPiranha implements Piranha, Runnable {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ServerPiranha.class.getPackageName());

    /**
     * Stores the one and only instance of the server.
     */
    private static ServerPiranha INSTANCE;

    /**
     * Stores the SSL flag.
     */
    private boolean ssl = false;

    /**
     * Get the instance.
     *
     * @return the instance.
     */
    public static ServerPiranha get() {
        return INSTANCE;
    }

    /**
     * Main method.
     *
     * @param arguments the arguments.
     */
    public static void main(String[] arguments) {
        Policy.setPolicy(new GlobalPolicy());
        setInitialContextFactory(GlobalInitialContextFactory.class);

        INSTANCE = new ServerPiranha();
        INSTANCE.processArguments(arguments);
        INSTANCE.run();
    }

    private static void setInitialContextFactory(Class<?> clazz) {
        System.setProperty("java.naming.factory.initial", GlobalInitialContextFactory.class.getName());
        GlobalPolicy.setContextId("ROOT server");
        try {
            new InitialContext().bind("name", "test");

            String test = (String) new InitialContext().lookup("name");

            if (!"test".equals(test)) {
                throw new IllegalStateException("Can't install required initial context factory");
            }

        } catch (NamingException e) {



            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Get the version.
     *
     * @return the version.
     */
    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    /**
     * Process the arguments.
     *
     * @param arguments the arguments.
     */
    private void processArguments(String[] arguments) {
        if (arguments != null) {
            for (String argument : arguments) {
                if (argument.equals("--ssl")) {
                    ssl = true;
                }
            }
        }
    }

    /**
     * Start method.
     */
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        LOGGER.log(INFO, () -> "Starting Piranha");

        DefaultWebApplicationServer webApplicationServer = new DefaultWebApplicationServer();
        DefaultHttpServer httpServer = new DefaultHttpServer(8080, webApplicationServer, ssl);
        httpServer.start();
        webApplicationServer.start();

        File[] webapps = new File("webapps").listFiles();
        if (webapps != null) {
            if (webapps.length != 0) {
                // Limit threads used by Weld, since default is Runtime.getRuntime().availableProcessors(), which is per deployment.
                int threadsPerApp = Math.max(2, Runtime.getRuntime().availableProcessors() / webapps.length);

                System.setProperty("org.jboss.weld.executor.threadPoolSize", threadsPerApp + "");
            }

            File deployingFile = createDeployingFile();

            Arrays.stream(webapps)
                  .parallel()
                  .filter(warFile -> warFile.getName().toLowerCase().endsWith(".war"))
                  .forEach(warFile -> deploy(warFile, webApplicationServer));

            deployingFile.delete();
        }

        long finishTime = System.currentTimeMillis();
        LOGGER.info("Started Piranha");
        LOGGER.log(INFO, "It took {0} milliseconds", finishTime - startTime);

        File startedFile = createStartedFile();

        File pidFile = new File("tmp/piranha.pid");
        while (httpServer.isRunning()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            if (!pidFile.exists()) {
                webApplicationServer.stop();
                httpServer.stop();
                startedFile.delete();
                System.exit(0);
            }
        }

        finishTime = System.currentTimeMillis();
        LOGGER.info("Stopped Piranha");
        LOGGER.log(INFO, "We ran for {0} milliseconds", finishTime - startTime);
    }


    private void deploy(File warFile, DefaultWebApplicationServer webApplicationServer) {
        String contextPath = getContextPath(warFile);

        MicroConfiguration configuration = new MicroConfiguration();
        configuration.setRoot(contextPath);
        configuration.setHttpStart(false);

        try {
            MicroWebApplication microWebApplication = new MicroWebApplication();
            microWebApplication.setContextPath(contextPath);

            GlobalPolicy.setContextId(microWebApplication.getServletContextId());

            microWebApplication.setDeployedApplication(
                new MicroOuterDeployer(configuration.postConstruct())
                    .deploy(ShrinkWrap.create(ZipImporter.class, warFile.getName()).importFrom(warFile).as(WebArchive.class))
                    .getDeployedApplication());

            webApplicationServer.addWebApplication(microWebApplication);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Failed to initialize app " + contextPath);
        } finally {
            GlobalPolicy.setContextId(null);
        }
    }

    private String getContextPath(File warFile) {
        String contextPath = warFile.getName().substring(0, warFile.getName().length() - 4);

        if (contextPath.equalsIgnoreCase("ROOT")) {
            contextPath = "";
        } else if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        return contextPath;
    }

    private File createDeployingFile() {
        File deployingFile = new File("webapps/deploying");
        try {
            deployingFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deployingFile;
    }

    private File createStartedFile() {
        File startedFile = new File("webapps/started");

        try {
            startedFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return startedFile;
    }
}
