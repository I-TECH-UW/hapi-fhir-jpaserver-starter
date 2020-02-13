FROM tomcat:9-jre11

RUN mkdir -p /data/hapi/lucenefiles && chmod 775 /data/hapi/lucenefiles
#Clean out unneccessary files from tomcat (especially pre-existing applications) 
RUN rm -rf /usr/local/tomcat/webapps/* \ 
    /usr/local/tomcat/conf/Catalina/localhost/manager.xml
    
#Add the application to tomcat
ADD target/*.war /usr/local/tomcat/webapps/
RUN ln -s /usr/local/tomcat/webapps/hapi-fhir-jpaserver /usr/local/tomcat/webapps/ROOT

#rewrite server.xml with our server.xml for a number of security configurations
#    
ADD server.xml /usr/local/tomcat/conf/server.xml

#rewrite cataline.properties with our catalina.properties so it contains:
#    org.apache.catalina.STRICT_SERVLET_COMPLIANCE=true
#    org.apache.catalina.connector.RECYCLE_FACADES=true
#    org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=false
#    org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=false
#    org.apache.coyote.USE_CUSTOM_STATUS_MSG_IN_HEADER=false
ADD install/tomcat-resources/catalina.properties /usr/local/tomcat/conf/catalina.properties

#replace ServerInfo.properties with a less informative one
RUN mkdir -p /usr/local/tomcat/lib/org/apache/catalina/util
ADD install/tomcat-resources/ServerInfo.properties /usr/local/tomcat/lib/org/apache/catalina/util/ServerInfo.properties 

#restrict files
#GID AND UID must be kept the same as setupTomcat.sh (if using default certificate group)
RUN groupadd tomcat; \
    groupadd tomcat-ssl-cert -g 8443; \ 
    useradd -M -s /bin/bash -u 8443 tomcat_admin; \
    usermod -a -G tomcat,tomcat-ssl-cert tomcat_admin; \
    chown -R tomcat_admin:tomcat $CATALINA_HOME; \
    chmod g-w,o-rwx $CATALINA_HOME; \
    chmod g-w,o-rwx $CATALINA_HOME/conf; \
    chmod o-rwx $CATALINA_HOME/logs; \
    chmod o-rwx $CATALINA_HOME/temp; \
    chmod g-w,o-rwx $CATALINA_HOME/bin; \
    chmod g-w,o-rwx $CATALINA_HOME/webapps; \
    chmod 770 $CATALINA_HOME/conf/catalina.policy; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/catalina.properties; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/context.xml; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/logging.properties; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/server.xml; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/tomcat-users.xml; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/web.xml

ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN chown tomcat_admin:tomcat /docker-entrypoint.sh; \
    chmod 770 /docker-entrypoint.sh;
    
USER tomcat_admin

ENTRYPOINT [ "/docker-entrypoint.sh" ]