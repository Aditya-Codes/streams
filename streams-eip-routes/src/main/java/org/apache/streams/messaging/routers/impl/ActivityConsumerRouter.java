package org.apache.streams.messaging.routers.impl;


import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.streams.messaging.routers.ActivityRouteBuilder;



import org.apache.streams.osgi.components.activityconsumer.ActivityConsumerWarehouse;
import org.apache.streams.osgi.components.activityconsumer.ActivityConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.camel.Header;
import org.apache.camel.Exchange;
import org.apache.camel.Exchange;
import org.apache.camel.CamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.camel.ExchangePattern;



public class ActivityConsumerRouter extends RouteBuilder implements ActivityRouteBuilder {
    private static final transient Log LOG = LogFactory.getLog(ActivityConsumerRouter.class);


    protected CamelContext camelContext;

    private ActivityConsumerWarehouse activityConsumerWarehouse;

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public void setActivityConsumerWarehouse(ActivityConsumerWarehouse activityConsumerWarehouse) {
        this.activityConsumerWarehouse = activityConsumerWarehouse;
    }


    public void createNewRouteForConsumer(Exchange exchange, ActivityConsumer activityConsumer){

        //todo: make this driven by configuration, and some better scheme then getCount for URL...
        //todo: make the route again if consumer exists...
        ActivityConsumer existingConsumer = activityConsumerWarehouse.findConsumerBySrc(activityConsumer.getSrc());

        if (existingConsumer==null){

            activityConsumer.setInRoute("http://localhost:8000/streams/publish/" + activityConsumerWarehouse.getConsumersCount());
            activityConsumerWarehouse.register(activityConsumer);

            try{
                //setup a message queue for this consumer.getInRoute()
                camelContext.addRoutes(new DynamcConsumerRouteBuilder(camelContext, "jetty:" + activityConsumer.getInRoute(), activityConsumer));
                //set the body to the url the producer should post to
                exchange.getOut().setBody(activityConsumer.getInRoute());
                LOG.info("all messages sent from " + activityConsumer.getSrc() + " must be posted to " + activityConsumer.getInRoute());
            }catch (Exception e){
                exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE,500);
                exchange.getOut().setBody("error creating route: " + e);
                LOG.error("error creating route: " + e);
            }

        } else{

            exchange.getOut().setBody(existingConsumer.getInRoute());
        }

    }


    public void configure() throws java.lang.Exception{
        //nothing...set the context?

    }

    /**
     * This route builder is a skeleton to add new routes at runtime
     */
    private static final class DynamcConsumerRouteBuilder extends RouteBuilder {
        private final String from;
        private ActivityConsumer activityConsumer;

        private DynamcConsumerRouteBuilder(CamelContext context, String from, ActivityConsumer activityConsumer) {
            super(context);
            this.from = from;
            this.activityConsumer = activityConsumer;
        }

        @Override
        public void configure() throws Exception {

            //todo: this message to the bean is always NULL!!!
            from(from)
                    .bean(activityConsumer, "receive").setBody(body())
                    .split()
                    .method(activityConsumer, "split")
                    .to("direct:activityQ");


        }
    }

}