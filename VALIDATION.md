# Validação da versão 0.3.0

Executada em 7 de setembro de 2026.

- **36 testes aprovados**, sem falhas: 9 de formatação, 10 do repositório, 6 de EPUB, 4 de interface, 6 de paginação e 1 de medidas reais.
- Os novos testes verificam os exemplos de Hemingway e Fitzgerald, maiúsculas/minúsculas, metadados prioritários, autores desconhecidos, nomes ambíguos e aplicação repetida da formatação.
- Importação de PDFs reais validada com nome de arquivo e com título/autor nos metadados. Biblioteca existente conserva ID, posição, deslocamento, página PDF, modo original, última leitura e conclusão.
- O primeiro teste revelou um caminho Windows inteiro no fallback de nome; o tratamento foi corrigido e os 36 testes passaram na repetição.
- Lint concluído sem erros. A compilação incremental encontrou um cache bloqueado pelo Windows; após remover somente esse cache, `assembleDebug` terminou com **BUILD SUCCESSFUL**.
- APK verificado: `versionCode=4`, `versionName=0.3.0`, assinatura SHA-256 `d1fcdcac54732c328bfdcc359031fb9b948d2dd8e8060aea14ae922b1dead053`, igual à versão local anterior.

Validação automatizada com Robolectric API 28, sem aparelho físico. O reconhecimento usa metadados e uma lista local limitada de autores; não identifica universalmente autores a partir de nomes de arquivo.

# Histórico: validação da versão 0.2.0

Executada em 7 de setembro de 2026, com o mesmo ambiente Android/JDK descrito abaixo.

- **24 testes aprovados**, sem falhas ou testes ignorados: 6 de EPUB, 7 do repositório, 4 de interface, 6 do algoritmo de paginação e 1 de medidas reais de texto.
- O teste de medidas cobre 12 combinações de tamanho de fonte, largura e altura, com escala de fonte do sistema ampliada. Verifica que todo caractere permanece no resultado e que os fragmentos cabem na página sem overflow.
- Os testes de interface executam deslizes para os dois lados, avanço por botão, troca entre rolagem e páginas, reabertura, mudança de fonte, primeira/última página e conclusão do livro. Conferem a preservação da posição dentro do trecho.
- Os testes de armazenamento verificam a preferência por páginas, posições independentes de dois livros e leitura do índice antigo sem o novo campo de deslocamento.
- `testDebugUnitTest lintDebug assembleDebug --continue`: **BUILD SUCCESSFUL**. Lint: 0 erros e 10 avisos de dependências/sugestões de API.
- Capturas `reader-paged.png` e `reader-paged-large-font.png` foram geradas e inspecionadas visualmente, além das capturas anteriores.
- APK confirmado com `versionCode=2`, `versionName=0.2.0` e a mesma assinatura SHA-256 do APK local 0.1.0. Pode atualizar essa instalação sem desinstalar.

A primeira tentativa foi interrompida por dois diretórios temporários de build que o Windows não conseguiu remover. Após limpar somente esses temporários, a execução completa passou. Não foi necessário alterar arquivos da biblioteca do usuário.

Os testes gráficos continuam sendo executados em Android simulado por Robolectric (API 28); não equivalem a uma instalação e teste em aparelho físico. O novo modo paginado se aplica ao texto de EPUBs e PDFs extraídos; o visualizador do PDF original mantém seus controles próprios.

## Histórico: versão 0.1.0

Executada em 6 de setembro de 2026, com JDK 17, Gradle 8.11.1, Android SDK 35 e Windows.

## Verificações executadas

`testDebugUnitTest`: **13 testes aprovados**, sem falhas ou testes ignorados.

- 6 testes do processamento EPUB e dos trechos: ordem do spine, metadados, entidades HTML, caminhos inválidos, capítulo ausente, criptografia, preservação de palavras e percentual.
- 5 testes do repositório em Android API 28 simulado por Robolectric: posições independentes e preferências após reabertura, recuperação do backup atômico, importação de um PDF real, duplicatas, preservação da cópia local, PDF sem texto e limpeza de uma importação inválida.
- 2 testes Compose: estado vazio com ação de importar; abertura de livro, troca de tema, retorno à estante e cartão de continuar.

`lintDebug`: sem erros. Avisos de versões mais recentes das dependências, sugestões de uso de KTX e código TLS transitivo do Bouncy Castle (dependência do PDFBox). O app não cria clientes de rede nem solicita permissão de internet; os avisos permanecem visíveis no relatório, sem supressão global.

`assembleDebug`: APK Android gerado em `app/build/outputs/apk/debug/app-debug.apk`.

O manifesto do APK também foi inspecionado com `aapt dump permissions`: não há `android.permission.INTERNET` nem permissão de acesso amplo ao armazenamento.

## Inspeção visual

Capturas renderizadas pelos testes Robolectric foram abertas e conferidas: biblioteca vazia em sépia, leitor em sépia e biblioteca no tema escuro. As imagens estão em `app/build/outputs/screenshots/` e são incluídas nos artefatos de verificação do workflow. Usam livros de teste, não arquivos pessoais do usuário.

As falhas iniciais de sincronização do teste Compose e da captura via PixelCopy foram corrigidas. Os testes exercitam o clique no cartão e a abertura real pelo ViewModel/repositório; não substituem essa navegação por um estado de tela estático.

## Limites da validação

Não havia aparelho Android ou emulador conectado. Não foi executado teste em hardware real, seletor de documentos de fabricantes, TalkBack, rotação física ou gestos no PDF nativo. A validação de PDF automatizada cobre extração e armazenamento; o renderizador original e seus gestos precisam de conferência no aparelho.

O roteiro manual está no README. O APK é assinado para desenvolvimento e serve para instalação e testes pessoais. Os testes não demonstram compatibilidade com todos os PDFs/EPUBs existentes, especialmente arquivos escaneados, multicolunas, protegidos ou malformados.
