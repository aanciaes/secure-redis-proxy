FROM jboss/keycloak

COPY tls/keycloak-tls.key /etc/x509/https/tls.key
COPY tls/keycloak-tls.crt /etc/x509/https/tls.crt

ENTRYPOINT [ "/opt/jboss/tools/docker-entrypoint.sh" ]

CMD ["-b", "0.0.0.0"]
