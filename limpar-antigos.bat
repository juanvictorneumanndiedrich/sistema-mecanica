@echo off
cd /d "%~dp0src\main\java\com\mecanica"
echo Apagando os arquivos antigos em portugues...
echo.
del /s /q *Equipamento*.java *Fornecedor*.java *Funcionario*.java *Servico*.java *Movimento*.java *Retirada*.java FormaPagamentoCompra.java
echo.
echo Pronto. Agora no Eclipse: F5 no projeto e depois Project ^> Clean.
pause
