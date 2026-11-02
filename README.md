# Embrace+

Aplicação Java Web acadêmica para gerenciamento de famílias, doações e entregas.

## Requisitos

- JDK 21 ou superior
- Não é necessário instalar Maven: o projeto inclui Maven Wrapper.

## Executar em desenvolvimento

No Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd jetty:run
```

No Linux ou macOS:

```bash
./mvnw clean test
./mvnw jetty:run
```

Acesse `http://localhost:8080/`. O endpoint técnico de verificação fica em
`http://localhost:8080/api/health`.

O banco é criado automaticamente em `data/doalocal.db`. Para usar outro local,
defina a variável de ambiente `DOALOCAL_DATABASE_PATH` ou a propriedade Java
`doalocal.database.path`. A propriedade Java tem prioridade.

## Gerar o WAR

```powershell
.\mvnw.cmd clean package
```

O artefato será gerado em `target/doalocal.war` e é compatível com containers
Jakarta Servlet 6.0, como Jetty 12 EE10 e Tomcat 10.1.
