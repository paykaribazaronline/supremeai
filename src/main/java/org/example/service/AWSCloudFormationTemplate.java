package org.example.service;

import org.example.model.CloudDeploymentConfig;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * Service for AWS CloudFormation infrastructure-as-code
 * Generates and manages CloudFormation templates
 */
@Service
public class AWSCloudFormationTemplate {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Generate CloudFormation template for ECS deployment
     */
    public String generateECSTemplate(CloudDeploymentConfig config) {
        Map<String, Object> template = new LinkedHashMap<>();
        
        template.put("AWSTemplateFormatVersion", "2010-09-09");
        template.put("Description", "SupremeAI Auto-Fix Agent System - ECS Deployment");
        
        Map<String, Object> parameters = new LinkedHashMap<>();
        
        Map<String, Object> environmentParam = new LinkedHashMap<>();
        environmentParam.put("Type", "String");
        environmentParam.put("Default", config.getEnvironment().name());
        environmentParam.put("Description", "Environment name");
        parameters.put("Environment", environmentParam);
        
        Map<String, Object> imageParam = new LinkedHashMap<>();
        imageParam.put("Type", "String");
        imageParam.put("Default", config.getImageTag());
        imageParam.put("Description", "Docker image tag");
        parameters.put("ImageTag", imageParam);
        
        template.put("Parameters", parameters);
        
        // Resources
        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("ECSCluster", createECSCluster(config));
        resources.put("TaskDefinition", createTaskDefinition(config));
        resources.put("ECSService", createECSService(config));
        resources.put("LoadBalancer", createLoadBalancer(config));
        resources.put("LogGroup", createLogGroup(config));
        resources.put("AutoScalingTarget", createAutoScalingTarget(config));
        resources.put("ScaleUpPolicy", createScaleUpPolicy());
        resources.put("ScaleDownPolicy", createScaleDownPolicy());
        
        template.put("Resources", resources);
        
        // Outputs
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("ClusterName", createOutput("ECSCluster", "Cluster name", "Ref"));
        outputs.put("ServiceName", createOutput("ECSService", "Service name", "GetAtt", "ServiceName"));
        outputs.put("LoadBalancerDNS", createOutput("LoadBalancer", "Load balancer DNS", "GetAtt", "DNSName"));
        outputs.put("LogGroupName", createOutput("LogGroup", "CloudWatch log group", "Ref"));
        
        template.put("Outputs", outputs);
        
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * Generate CloudFormation template for EC2 deployment
     */
    public String generateEC2Template(CloudDeploymentConfig config) {
        Map<String, Object> template = new LinkedHashMap<>();
        
        template.put("AWSTemplateFormatVersion", "2010-09-09");
        template.put("Description", "SupremeAI Auto-Fix Agent System - EC2 Deployment");
        
        Map<String, Object> resources = new LinkedHashMap<>();
        
        resources.put("VPC", createVPC(config));
        resources.put("Subnet", createSubnet(config));
        resources.put("InternetGateway", createInternetGateway());
        resources.put("SecurityGroup", createSecurityGroup(config));
        resources.put("EC2Instance", createEC2Instance(config));
        resources.put("ElasticIP", createElasticIP(config));
        
        template.put("Resources", resources);
        
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("InstancePublicIP", createOutput("ElasticIP", "Instance public IP", "Ref"));
        outputs.put("SecurityGroupId", createOutput("SecurityGroup", "Security group ID", "Ref"));
        
        template.put("Outputs", outputs);
        
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * Generate CloudFormation template for RDS database
     */
    public String generateRDSTemplate(CloudDeploymentConfig config) {
        Map<String, Object> template = new LinkedHashMap<>();
        
        template.put("AWSTemplateFormatVersion", "2010-09-09");
        template.put("Description", "SupremeAI Postgres Database - RDS");
        
        Map<String, Object> resources = new LinkedHashMap<>();
        
        Map<String, Object> dbInstance = new LinkedHashMap<>();
        dbInstance.put("Type", "AWS::RDS::DBInstance");
        Map<String, Object> dbProps = new LinkedHashMap<>();
        dbProps.put("Engine", "postgres");
        dbProps.put("EngineVersion", "14.7");
        dbProps.put("DBInstanceClass", "db.t3.micro");
        dbProps.put("AllocatedStorage", "100");
        dbProps.put("DBInstanceIdentifier", "supremeai-db");
        dbProps.put("MasterUsername", "admin");
        dbProps.put("MasterUserPassword", "{{resolve:secretsmanager:supremeai-db-secret:SecretString:password}}");
        dbProps.put("BackupRetentionPeriod", 7);
        dbProps.put("StorageEncrypted", true);
        dbProps.put("EnableCloudwatchLogsExports", Arrays.asList("postgresql"));
        dbInstance.put("Properties", dbProps);
        
        resources.put("DBInstance", dbInstance);
        resources.put("DBSecurityGroup", createDBSecurityGroup(config));
        
        template.put("Resources", resources);
        
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * Deployment result
     */
    public CloudFormationDeploymentResult deployTemplate(String templateName, String templateBody, CloudDeploymentConfig config) {
        CloudFormationDeploymentResult result = new CloudFormationDeploymentResult();
        result.stackName = templateName + "-" + config.getEnvironment().name();
        result.deploymentId = config.getDeploymentId();
        result.startTime = System.currentTimeMillis();
        
        try {
            result.stackStatus = "CREATE_IN_PROGRESS";
            result.templateBody = templateBody;
            result.region = config.getAwsConfig().region;
            
            // Simulate deployment
            result.stackStatus = "CREATE_COMPLETE";
            result.stackArn = String.format("arn:aws:cloudformation:%s:%s:stack/%s/%s",
                result.region,
                config.getAwsConfig().accountId,
                result.stackName,
                UUID.randomUUID().toString());
            result.resourcesCreated = 8;
            
            result.endTime = System.currentTimeMillis();
            result.duration = result.endTime - result.startTime;
            
        } catch (Exception e) {
            result.stackStatus = "CREATE_FAILED";
            result.errorMessage = e.getMessage();
        }
        
        return result;
    }
    
    // Helper methods for resource creation
    
    private Map<String, Object> createECSCluster(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ECS::Cluster");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("ClusterName", "supremeai-" + config.getEnvironment().name().toLowerCase());
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createTaskDefinition(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ECS::TaskDefinition");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("Family", "supremeai");
        props.put("NetworkMode", "awsvpc");
        props.put("RequiresCompatibilities", Arrays.asList("FARGATE"));
        props.put("Cpu", String.valueOf((int)(config.getCpuLimit() * 1024)));
        props.put("Memory", String.valueOf((int)config.getMemoryLimit()));
        
        Map<String, Object> container = new LinkedHashMap<>();
        container.put("Name", "supremeai");
        container.put("Image", config.getImageTag());
        container.put("PortMappings", Arrays.asList(
            Collections.singletonMap("ContainerPort", 8080)
        ));
        container.put("LogConfiguration", createLogConfig(config));
        
        props.put("ContainerDefinitions", Arrays.asList(container));
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createECSService(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ECS::Service");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("ServiceName", "supremeai");
        props.put("Cluster", Collections.singletonMap("Ref", "ECSCluster"));
        props.put("TaskDefinition", Collections.singletonMap("Ref", "TaskDefinition"));
        props.put("DesiredCount", config.getInstanceCount());
        props.put("LaunchType", "FARGATE");
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createLoadBalancer(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ElasticLoadBalancingV2::LoadBalancer");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("Name", "supremeai-alb");
        props.put("Type", "application");
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createLogGroup(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::Logs::LogGroup");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("LogGroupName", "/ecs/supremeai-" + config.getEnvironment().name().toLowerCase());
        props.put("RetentionInDays", 7);
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createAutoScalingTarget(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ApplicationAutoScaling::ScalableTarget");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("MaxCapacity", config.getMaxReplicas());
        props.put("MinCapacity", config.getMinReplicas());
        props.put("ResourceId", "service/supremeai/supremeai");
        props.put("RoleARN", "arn:aws:iam::ACCOUNT:role/ecsAutoscaleRole");
        props.put("ScalableDimension", "ecs:service:DesiredCount");
        props.put("ServiceNamespace", "ecs");
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createScaleUpPolicy() {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ApplicationAutoScaling::ScalingPolicy");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("PolicyName", "scale-up");
        props.put("PolicyType", "TargetTrackingScaling");
        props.put("ScalingTargetId", Collections.singletonMap("Ref", "AutoScalingTarget"));
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createScaleDownPolicy() {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::ApplicationAutoScaling::ScalingPolicy");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("PolicyName", "scale-down");
        props.put("PolicyType", "StepScaling");
        props.put("ScalingTargetId", Collections.singletonMap("Ref", "AutoScalingTarget"));
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createVPC(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::VPC");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("CidrBlock", config.getAwsConfig().vpcCidr);
        props.put("EnableDnsHostnames", true);
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createSubnet(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::Subnet");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("VpcId", Collections.singletonMap("Ref", "VPC"));
        props.put("CidrBlock", config.getAwsConfig().subnetCidr);
        props.put("AvailabilityZone", "us-east-1a");
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createInternetGateway() {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::InternetGateway");
        return resource;
    }
    
    private Map<String, Object> createSecurityGroup(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::SecurityGroup");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("GroupDescription", "SupremeAI Security Group");
        props.put("VpcId", Collections.singletonMap("Ref", "VPC"));
        props.put("SecurityGroupIngress", Arrays.asList(
            createSecurityGroupIngress(8080, "80"),
            createSecurityGroupIngress(443, "443")
        ));
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createSecurityGroupIngress(int fromPort, String description) {
        Map<String, Object> ingress = new LinkedHashMap<>();
        ingress.put("IpProtocol", "tcp");
        ingress.put("FromPort", fromPort);
        ingress.put("ToPort", fromPort);
        ingress.put("CidrIp", "0.0.0.0/0");
        ingress.put("Description", description);
        return ingress;
    }
    
    private Map<String, Object> createEC2Instance(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::Instance");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("ImageId", "ami-0c55b159cbfafe1f0");
        props.put("InstanceType", config.getInstanceType());
        props.put("SecurityGroupIds", Collections.singletonList(Collections.singletonMap("Ref", "SecurityGroup")));
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createElasticIP(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::EIP");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("InstanceId", Collections.singletonMap("Ref", "EC2Instance"));
        props.put("Domain", "vpc");
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createDBSecurityGroup(CloudDeploymentConfig config) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("Type", "AWS::EC2::SecurityGroup");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("GroupDescription", "RDS Security Group");
        props.put("SecurityGroupIngress", Collections.singletonList(
            createSecurityGroupIngress(5432, "PostgreSQL")
        ));
        resource.put("Properties", props);
        return resource;
    }
    
    private Map<String, Object> createLogConfig(CloudDeploymentConfig config) {
        Map<String, Object> logConfig = new LinkedHashMap<>();
        logConfig.put("LogDriver", "awslogs");
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("awslogs-group", "/ecs/supremeai");
        options.put("awslogs-region", config.getAwsConfig().region);
        options.put("awslogs-stream-prefix", "ecs");
        logConfig.put("Options", options);
        return logConfig;
    }
    
    private Map<String, Object> createOutput(String logicalId, String description, String... getType) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("Description", description);
        
        if (getType.length == 1 && "Ref".equals(getType[0])) {
            output.put("Value", Collections.singletonMap("Ref", logicalId));
        } else if (getType.length >= 2) {
            Map<String, Object> getAtt = new LinkedHashMap<>();
            getAtt.put("Fn::GetAtt", Arrays.asList(logicalId, getType[1]));
            output.put("Value", getAtt);
        }
        
        return output;
    }
    
    // Result class
    
    public static class CloudFormationDeploymentResult {
        public String stackName;
        public String deploymentId;
        public String templateBody;
        public String stackStatus;       // CREATE_IN_PROGRESS, CREATE_COMPLETE, CREATE_FAILED
        public String stackArn;
        public String region;
        public int resourcesCreated;
        public long startTime;
        public long endTime;
        public long duration;
        public String errorMessage;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("stackName", stackName);
            map.put("status", stackStatus);
            map.put("region", region);
            map.put("resourcesCreated", resourcesCreated);
            map.put("duration", duration);
            if (stackArn != null) {
                map.put("stackArn", stackArn);
            }
            return map;
        }
    }
}
