# Servidor - Sistema de Grupos

Aplicação servidora em Java que gerencia grupos, contadores de utilização e logs de acesso via comunicação por sockets.

## Arquitetura do Servidor

### Classes Principais

- **`Servidor.java`** - Classe principal que gerencia conexões e threads
- **`ContadorGrupo.java`** - Modelo de dados para contadores de grupos
- **`LogGrupo.java`** - Modelo de dados para logs de acesso

### Estruturas de Dados em Memória

```java
// Lista de contadores - inicializada com 10 grupos e 0 utilizações
private static List<ContadorGrupo> contadores = new ArrayList<>();

// Lista de logs - armazena timestamp de cada acesso
private static List<LogGrupo> logs = new ArrayList<>();
```

### Grupos Pré-definidos

| ID | Nome        | Utilizações Iniciais |
|----|-------------|---------------------|
| 1  | Financeiro  | 0                   |
| 2  | Apostas     | 0                   |
| 3  | Fotografia  | 0                   |
| 4  | Corrida     | 0                   |
| 5  | Aulas       | 0                   |
| 6  | Treinamentos| 0                   |
| 7  | Pets        | 0                   |
| 8  | Viagens     | 0                   |
| 9  | Blackjack   | 0                   |
| 10 | RPG         | 0                   |

## Protocolo de Comunicação Socket

### Configuração de Conexão
```
Servidor: localhost (127.0.0.1) ou IP remoto
Porta: 12345
Protocolo: TCP Socket com ObjectStreams
```

### Fluxo Completo de Comunicação

#### 📡 **PASSO 1: Cliente Estabelece Conexão**
```java
// Cliente cria socket para conectar ao servidor
Socket clienteSocket = new Socket("127.0.0.1", 12345);
// OU para servidor remoto:
// Socket clienteSocket = new Socket("IP_DO_SERVIDOR", 12345);
```

#### 📤 **PASSO 2: Cliente Envia ID do Grupo**
O cliente **DEVE** enviar um objeto `Integer` contendo o ID do grupo (1-10):

```java
// Criar stream de saída para enviar objetos
ObjectOutputStream saida = new ObjectOutputStream(clienteSocket.getOutputStream());

// Enviar ID do grupo (OBRIGATÓRIO: Integer de 1 a 10)
Integer idGrupo = 5; // Exemplo: grupo Aulas
saida.writeObject(idGrupo);

// IMPORTANTE: Não fechar o stream ainda!
```

#### ⚙️ **PASSO 3: Servidor Processa a Requisição**
1. **Validação:** Servidor verifica se ID está entre 1 e 10
2. **Incremento:** Aumenta contador do grupo solicitado
3. **Log:** Registra timestamp da operação
4. **Preparação:** Monta dados para resposta

#### 📥 **PASSO 4: Cliente Recebe Resposta (2 Objetos)**
O servidor envia **EXATAMENTE 2 OBJETOS** em sequência. O cliente deve receber ambos:

```java
// Criar stream de entrada para receber objetos
ObjectInputStream entrada = new ObjectInputStream(clienteSocket.getInputStream());

// PRIMEIRO OBJETO: Lista completa de contadores (ranking)
@SuppressWarnings("unchecked")
List<ContadorGrupo> rankingCompleto = (List<ContadorGrupo>) entrada.readObject();

// SEGUNDO OBJETO: Logs filtrados apenas do grupo solicitado
@SuppressWarnings("unchecked")
List<LogGrupo> logsDoGrupo = (List<LogGrupo>) entrada.readObject();
```

#### 🔚 **PASSO 5: Cliente Fecha Conexão**
```java
// Fechar streams e conexão
entrada.close();
saida.close();
clienteSocket.close();
```

## Como Implementar o Cliente

### Exemplo Completo de Cliente

```java
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClienteExemplo {
    public static void main(String[] args) {
        int idGrupo = 3; // ID do grupo desejado (1-10)
        String servidor = "127.0.0.1"; // IP do servidor
        int porta = 12345;
        
        try (Socket clienteSocket = new Socket(servidor, porta)) {
            System.out.println("✅ Conectado ao servidor: " + servidor + ":" + porta);
            
            // PASSO 1: Enviar ID do grupo
            ObjectOutputStream saida = new ObjectOutputStream(clienteSocket.getOutputStream());
            saida.writeObject(idGrupo);
            System.out.println("📤 Enviado ID do grupo: " + idGrupo);
            
            // PASSO 2: Receber ranking completo
            ObjectInputStream entrada = new ObjectInputStream(clienteSocket.getInputStream());
            @SuppressWarnings("unchecked")
            List<ContadorGrupo> ranking = (List<ContadorGrupo>) entrada.readObject();
            System.out.println("📥 Recebido ranking com " + ranking.size() + " grupos");
            
            // PASSO 3: Receber logs do grupo
            @SuppressWarnings("unchecked")
            List<LogGrupo> logs = (List<LogGrupo>) entrada.readObject();
            System.out.println("📥 Recebidos " + logs.size() + " logs do grupo " + idGrupo);
            
            // PASSO 4: Processar dados recebidos
            processarRanking(ranking);
            processarLogs(logs);
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Erro na comunicação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void processarRanking(List<ContadorGrupo> ranking) {
        System.out.println("\n=== RANKING DOS GRUPOS ===");
        for (ContadorGrupo grupo : ranking) {
            System.out.println("ID " + grupo.getIdGrupo() + 
                             " - " + grupo.getNomeGrupo() + 
                             ": " + grupo.getQuantidadeUtilizacoes() + " utilizações");
        }
    }
    
    private static void processarLogs(List<LogGrupo> logs) {
        System.out.println("\n=== LOGS DO GRUPO ===");
        if (logs.isEmpty()) {
            System.out.println("Nenhum log encontrado para este grupo.");
        } else {
            for (LogGrupo log : logs) {
                System.out.println("Acesso em: " + log.getTimestamp());
            }
        }
    }
}
```

### Tratamento de Erros Comuns

#### ❌ **Erro: ID Inválido**
```java
// Se enviar ID fora do range 1-10
Integer idInvalido = 15;
saida.writeObject(idInvalido);
// Servidor fecha conexão sem resposta
```

#### ❌ **Erro: Servidor Indisponível**
```java
try {
    Socket socket = new Socket("IP_INEXISTENTE", 12345);
} catch (ConnectException e) {
    System.err.println("Servidor não encontrado ou offline");
}
```

#### ❌ **Erro: Ordem Incorreta de Recebimento**
```java
// ERRADO: Tentar receber apenas um objeto
Object obj = entrada.readObject(); // Vai receber apenas o ranking

// CORRETO: Receber os dois objetos em sequência
List<ContadorGrupo> ranking = (List<ContadorGrupo>) entrada.readObject();
List<LogGrupo> logs = (List<LogGrupo>) entrada.readObject();
```

## Modelos de Dados (Classes a Implementar no Cliente)

### ContadorGrupo
```java
import java.io.Serializable;

public class ContadorGrupo implements Serializable {
    private int idGrupo;           // ID do grupo (1-10)
    private String nomeGrupo;      // Nome do grupo
    private int quantidadeUtilizacoes; // Contador de utilizações
    
    // Construtor padrão obrigatório para serialização
    public ContadorGrupo() {}
    
    public ContadorGrupo(int idGrupo, String nomeGrupo, int quantidadeUtilizacoes) {
        this.idGrupo = idGrupo;
        this.nomeGrupo = nomeGrupo;
        this.quantidadeUtilizacoes = quantidadeUtilizacoes;
    }
    
    // Getters e setters obrigatórios
    public int getIdGrupo() { return idGrupo; }
    public void setIdGrupo(int idGrupo) { this.idGrupo = idGrupo; }
    
    public String getNomeGrupo() { return nomeGrupo; }
    public void setNomeGrupo(String nomeGrupo) { this.nomeGrupo = nomeGrupo; }
    
    public int getQuantidadeUtilizacoes() { return quantidadeUtilizacoes; }
    public void setQuantidadeUtilizacoes(int quantidadeUtilizacoes) { 
        this.quantidadeUtilizacoes = quantidadeUtilizacoes; 
    }
}
```

### LogGrupo
```java
import java.io.Serializable;

public class LogGrupo implements Serializable {
    private int idGrupo;      // ID do grupo (1-10)
    private String timestamp; // Data/hora no formato "yyyy-MM-dd HH:mm:ss"
    
    // Construtor padrão obrigatório para serialização
    public LogGrupo() {}
    
    public LogGrupo(int idGrupo, String timestamp) {
        this.idGrupo = idGrupo;
        this.timestamp = timestamp;
    }
    
    // Getters e setters obrigatórios
    public int getIdGrupo() { return idGrupo; }
    public void setIdGrupo(int idGrupo) { this.idGrupo = idGrupo; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
```

## Comportamento do Servidor

### Thread Safety
- Operações de incremento e logging são sincronizadas
- Suporte a múltiplos clientes simultâneos
- Cada cliente é atendido em thread separada

### Validação Rigorosa
```java
if (idGrupo < 1 || idGrupo > 10) {
    System.out.println("ID de grupo inválido: " + idGrupo);
    clienteSocket.close(); // Conexão fechada sem resposta
    return;
}
```

### Logs do Servidor
O servidor registra no console todas as operações:
```
Grupos inicializados:
ContadorGrupo{idGrupo=1, nomeGrupo='Financeiro', quantidadeUtilizacoes=0}
...
Servidor iniciado na porta 12345
Aguardando conexões...
Um cliente se conectou
Thread iniciada: Thread-1
ID do grupo solicitado: 3
Grupo Fotografia utilizado. Total: 1
Log adicionado: LogGrupo{idGrupo=3, timestamp='2024-12-19 14:30:25'}
Dados enviados para o cliente
```

## Execução do Servidor

### Compilação
```bash
cd IFES-Trabalho-Sockets-Server
javac -cp src src/sockets/thread/*.java
```

### Execução
```bash
java -cp src sockets.thread.Servidor
```

### Saída Esperada na Inicialização
```
Grupos inicializados:
ContadorGrupo{idGrupo=1, nomeGrupo='Financeiro', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=2, nomeGrupo='Apostas', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=3, nomeGrupo='Fotografia', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=4, nomeGrupo='Corrida', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=5, nomeGrupo='Aulas', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=6, nomeGrupo='Treinamentos', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=7, nomeGrupo='Pets', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=8, nomeGrupo='Viagens', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=9, nomeGrupo='Blackjack', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=10, nomeGrupo='RPG', quantidadeUtilizacoes=0}
Servidor iniciado na porta 12345
Aguardando conexões...
```

## Requisitos Técnicos

- **Java 8+** (para Streams e Collections)
- **Serialização** - Classes implementam `Serializable`
- **Thread Safety** - Uso de `synchronized` blocks
- **Formato de Data** - `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")`
- **TCP Sockets** - Comunicação confiável cliente-servidor

## Checklist para Implementação do Cliente

### ✅ Antes de Conectar
- [ ] Implementar classes `ContadorGrupo` e `LogGrupo` idênticas ao servidor
- [ ] Definir ID do grupo desejado (1-10)
- [ ] Configurar IP e porta do servidor

### ✅ Durante a Conexão
- [ ] Criar `Socket` para o servidor
- [ ] Criar `ObjectOutputStream` para envio
- [ ] Enviar `Integer` com ID do grupo
- [ ] Criar `ObjectInputStream` para recebimento
- [ ] Receber primeiro objeto: `List<ContadorGrupo>`
- [ ] Receber segundo objeto: `List<LogGrupo>`

### ✅ Após Receber Dados
- [ ] Processar ranking completo
- [ ] Processar logs do grupo específico
- [ ] Fechar streams e conexão
- [ ] Tratar exceções adequadamente

## Considerações Importantes

1. **Ordem Obrigatória:** Sempre enviar ID do grupo ANTES de receber dados
2. **Dois Objetos:** Servidor sempre envia ranking + logs (nessa ordem)
3. **Thread Safety:** Servidor suporta múltiplos clientes simultâneos
4. **Persistência:** Dados mantidos em memória durante execução do servidor
5. **Validação:** IDs fora do range 1-10 resultam em conexão fechada
6. **Logs Filtrados:** Cliente recebe apenas logs do grupo solicitado
7. **Serialização:** Classes devem implementar `Serializable` corretamente 