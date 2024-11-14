package ca.petsearch.metrics;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;
 
public class StdoutEnvironment implements Environment {
    private static final Logger log = LoggerFactory.getLogger(StdoutEnvironment.class);
    private ISink sink;
    private final Configuration config;
 
    public StdoutEnvironment(Configuration config) {
        this.config = config;
    }
 
    public boolean probe() {
        return false;
    }
 
    public String getName() {
        if (this.config.getServiceName().isPresent()) {
            return this.config.getServiceName().get();
        } else {
            log.info("Unknown name");
            return "Unknown";
        }
    }
 
    public String getType() {
        if (this.config.getServiceType().isPresent()) {
            return this.config.getServiceType().get();
        } else {
            log.info("Unknown type");
            return "Unknown";
        }
    }
 
    public String getLogGroupName() {
        return this.config.getLogGroupName().orElse(this.getName() + "-metrics");
    }
 
    public void configureContext(MetricsContext context) {
    }
 
    public ISink getSink() {
        if (this.sink == null) {
            this.sink = new ConsoleSink();
        }
 
        return this.sink;
    }
}