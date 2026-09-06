# Validação da versão 0.1.0

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
