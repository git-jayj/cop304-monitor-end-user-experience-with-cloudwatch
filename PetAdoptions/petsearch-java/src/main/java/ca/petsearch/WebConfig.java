package ca.petsearch;

import ca.petsearch.metrics.StdoutEnvironment;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${aws.local.endpoint:#{null}}")
    private String endpoint = "";

    @Value("${cloud.aws.region.static:#{null}}")
    private String region = "";

    @Bean
    public RandomNumberGenerator randomNumberGenerator() {
        return new PseudoRandomNumberGenerator();
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    @Bean
    public Tracer tracer(OpenTelemetry otel) {
        return otel.getTracer("petsearch");
    }

    @Bean
    public AmazonS3 amazonS3() {
        return withLocalEndpoint(AmazonS3ClientBuilder.standard())
                .build();
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return withLocalEndpoint(AmazonDynamoDBClientBuilder.standard())
                .build();
    }

    @Bean
    public AWSSimpleSystemsManagement awsSimpleSystemsManagement() {
        return withLocalEndpoint(AWSSimpleSystemsManagementClientBuilder.standard())
                .build();
    }

    private <Subclass extends AwsClientBuilder<Subclass, ?>> Subclass withLocalEndpoint(Subclass builder) {
        return endpoint.isEmpty() ? builder : builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region));
    }

    @Bean
    public MetricsLogger metricsLogger() {
        software.amazon.cloudwatchlogs.emf.config.Configuration config = EnvironmentConfigurationProvider.getConfig();
        Environment environment = new StdoutEnvironment(config);
        MetricsLogger metricsLogger = new MetricsLogger(environment);
        metricsLogger.setNamespace("PetClinic/Search");
        metricsLogger.resetDimensions(false);
        return metricsLogger;
    }
 
    @Bean
    public MetricEmitter metricEmitter(OpenTelemetry otel, MetricsLogger metricsLogger) {
        return new MetricEmitter(otel, metricsLogger);
    }

    @Bean
    public FilterRegistrationBean<ApplicationFilter> filterRegistrationBean(MetricEmitter metricEmitter) {
        FilterRegistrationBean<ApplicationFilter> filterBean = new FilterRegistrationBean<>();
        filterBean.setFilter(new ApplicationFilter(metricEmitter));
        filterBean.setUrlPatterns(Arrays.asList("/api/search"));
        return filterBean;
    }

}
