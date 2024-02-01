package org.springframework.samples.petclinic.config;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.session.FlushMode;
import org.springframework.session.MapSession;
import org.springframework.session.SaveMode;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.hazelcast.Hazelcast4IndexedSessionRepository;
import org.springframework.session.hazelcast.Hazelcast4PrincipalNameExtractor;
import org.springframework.session.hazelcast.HazelcastSessionSerializer;
import org.springframework.session.hazelcast.config.annotation.SpringSessionHazelcastInstance;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import com.hazelcast.config.AttributeConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.IndexConfig;
import com.hazelcast.config.IndexType;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Configuration
@EnableHazelcastHttpSession
//@PropertySource("classpath:session/session.properties")
public class SessionConfiguration {
	private final String SESSIONS_MAP_NAME = "spring-session-map-name";

    
//	@Value("${session.members}")
//	private List<String> members;
	
	@Bean
    public SessionRepositoryCustomizer<Hazelcast4IndexedSessionRepository> customize() {
        return (sessionRepository) -> {
            sessionRepository.setFlushMode(FlushMode.IMMEDIATE);
            sessionRepository.setSaveMode(SaveMode.ALWAYS);
            sessionRepository.setSessionMapName(SESSIONS_MAP_NAME);
            sessionRepository.setDefaultMaxInactiveInterval(900);
        };
    }

    @Bean
    @SpringSessionHazelcastInstance

   public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName("spring-session-cluster");
 
        // Add this attribute to be able to query sessions by their PRINCIPAL_NAME_ATTRIBUTE's
        AttributeConfig attributeConfig = new AttributeConfig()
                .setName(Hazelcast4IndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE)
                .setExtractorClassName(Hazelcast4PrincipalNameExtractor.class.getName());
 
        // Configure the sessions map
        config.getMapConfig(SESSIONS_MAP_NAME)
                .addAttributeConfig(attributeConfig).addIndexConfig(
                new IndexConfig(IndexType.HASH, Hazelcast4IndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE));
 
        
        // Use custom serializer to de/serialize sessions faster. 
        SerializerConfig serializerConfig = new SerializerConfig();
        serializerConfig.setImplementation(new HazelcastSessionSerializer()).setTypeClass(MapSession.class);
        config.getSerializationConfig().addSerializerConfig(serializerConfig);
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
       // joinConfig.getTcpIpConfig().setEnabled(true).setMembers(members);
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getKubernetesConfig().setEnabled(true)
        						.setProperty("namespace", "petclinic-kube")
        						.setProperty("service-name", "hz-service");
 
        return Hazelcast.newHazelcastInstance(config);
    }
 
   
    
    
    // Workaround for https://github.com/spring-projects/spring-session/issues/1040 and https://github.com/spring-projects/spring-framework/issues/22319
    @Bean
    public CookieSerializer cookieSerializer(ServletContext ctx) {
        DefaultCookieSerializer cs = new DefaultCookieSerializer();

        try {
            SessionCookieConfig cfg = ctx.getSessionCookieConfig();
            cs.setCookieName(cfg.getName());
            cs.setDomainName(cfg.getDomain());
            cs.setCookiePath(cfg.getPath());
            cs.setCookieMaxAge(cfg.getMaxAge());
        } catch (UnsupportedOperationException e) {
            cs.setCookieName("MY_SESSIONID");
            cs.setCookiePath(ctx.getContextPath());
        }

        return cs;
    }

}
