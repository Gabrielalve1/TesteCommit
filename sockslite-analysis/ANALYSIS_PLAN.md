# Plano de análise

## 0. Proveniência da amostra
1. Obter o APK por fonte confiável e respeitar termos/licenças aplicáveis.
2. Registrar origem, data, versão, tamanho e SHA-256.
3. Preservar a amostra original e trabalhar numa cópia.

## 1. Análise estática
- Manifesto: package name, SDK, componentes exportados e permissões.
- DEX/código: pacotes, classes, chamadas a serviços Android e fluxo de inicialização.
- Bibliotecas: dependências de rede, SSH/TLS, proxy e VPN, se presentes.
- Recursos: layouts, strings, configurações e formatos de importação/exportação.
- Código nativo: listar bibliotecas .so e suas arquiteturas; não presumir que foram compreendidas apenas pelo nome.

## 2. Mapear fluxos observáveis
Documentar, quando houver evidência: inicialização e permissões; seleção de perfil/servidor; validação de campos; estados desconectado/conectando/conectado/erro; armazenamento local; mensagens e logs.

## 3. Comunicação de rede
- Listar endpoints/domínios codificados ou documentados encontrados na amostra.
- Distinguir strings de URLs efetivamente chamadas.
- Não testar endpoints de terceiros nem interceptar tráfego sem autorização.
- Nunca publicar tokens, credenciais, identificadores pessoais ou configurações privadas.

## 4. Teste dinâmico autorizado
Se necessário, usar emulador próprio, conta de teste e rede controlada. Registrar passos, versão e resultados. Não contornar autenticação nem mecanismos de segurança.

## 5. Reconstrução independente
Implementar comportamentos genéricos e observáveis: interface, gestão de perfis próprios, estados e mensagens. Para VPN real, usar APIs de plataforma e protocolos documentados com servidores próprios. Não reutilizar chaves, credenciais, endpoints privados ou código proprietário.

## Critérios de conclusão
- Cada afirmação técnica aponta para arquivo/classe e evidência.
- Resultados automatizados foram revisados manualmente.
- Limitações e hipóteses estão claramente separadas.
- Nenhum segredo ou dado pessoal foi incluído no relatório.
