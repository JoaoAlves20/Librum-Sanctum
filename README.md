# Librum Sanctum

Biblioteca pessoal para Android, offline desde a primeira abertura. Sem conta, servidor, anúncios ou permissão de internet.

## Primeira versão

- Importa PDF e EPUB pelo seletor de arquivos e mantém uma cópia privada no aparelho.
- Estante com busca, capas tipográficas, último livro aberto, percentual e botão de continuar.
- Leitura de texto extraído com fonte clássica ou sem serifa, tamanho ajustável e temas claro, sépia e escuro.
- PDF original renderizado no aparelho, com páginas, zoom e arraste.
- Posição independente por livro, preferências persistidas e identificação de duplicatas pelo conteúdo.
- EPUB segue a ordem de leitura do `spine`, não a ordem dos arquivos no ZIP.

## Executar

Abra esta pasta no Android Studio com JDK 17, Android SDK 35 e Build Tools 35.0.0. Aguarde a sincronização do Gradle e execute o módulo `app` em Android 8.0 ou superior (API 26).

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

No Windows, use `gradlew.bat`. O APK de desenvolvimento será gerado em `app/build/outputs/apk/debug/app-debug.apk`. O workflow Android também compila, testa, analisa e disponibiliza o APK como artefato.

## Organização

Kotlin e Jetpack Compose para a interface; `LibraryViewModel` coordena estado e tarefas de disco; `LibraryRepository` mantém os arquivos e um índice JSON com escrita atômica. PDFBox Android extrai texto, `PdfRenderer` exibe o original, e Jsoup interpreta o conteúdo EPUB. Não há serviços externos em tempo de execução.

O texto é dividido em trechos de até 65 palavras. A retomada usa o primeiro trecho visível, portanto mudar fonte ou tamanho não invalida a posição. Ao reabrir, o início desse trecho aparece no topo (não o pixel exato). O percentual acompanha os trechos ou páginas ultrapassados; o botão de conclusão registra 100%. Alternar entre texto e PDF aproxima a posição pela página de origem.

## Limites conhecidos

- PDFs escaneados ficam disponíveis no modo original; esta versão não faz OCR.
- O texto reformata o conteúdo: imagens, tabelas, notas e diagramação complexa não são preservadas nesse modo. PDFs em múltiplas colunas podem ter ordem de extração imperfeita; use o original nesses casos.
- Sem suporte a DRM ou senha. EPUB com declaração de criptografia (inclusive fontes ofuscadas) é recusado nesta versão.
- Limite de 100 MB por arquivo, 3.000 páginas PDF e limites adicionais de texto descompactado para proteger a memória. Livros grandes podem levar tempo para importar.
- EPUBs convencionais em UTF-8 são o foco inicial. Não há suporte a áudio, scripts, layout fixo ou estilos editoriais.
- A desinstalação apaga a biblioteca privada. Os arquivos originais escolhidos pelo usuário não são alterados.
- O APK debug serve para testes; publicação em loja exige uma assinatura de release própria.

## Validação manual no Android

1. Ative o modo avião e importe um EPUB e um PDF que já estejam no aparelho.
2. Abra cada livro, avance, volte à estante, encerre o app e abra novamente. Confira as duas posições e o cartão de continuar.
3. Mude fonte, tamanho e tema; gire o aparelho e confira a retomada pelo trecho.
4. Alterne um PDF entre texto e original; avance páginas, aplique zoom e reabra.
5. Importe um PDF sem texto: deve abrir no modo original.
6. Reimporte o mesmo arquivo: não deve duplicar nem perder progresso.
7. Selecione um arquivo inválido: deve mostrar erro e preservar a biblioteca.
8. Use fonte do sistema ampliada e TalkBack para conferir navegação e legibilidade.

Os testes JVM incluem extração de PDF real, leitura de EPUB, rejeição de arquivos inválidos, duplicatas e persistência. Consulte `VALIDATION.md` para o que efetivamente foi executado nesta implementação.
