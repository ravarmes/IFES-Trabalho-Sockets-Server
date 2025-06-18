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

## Protocolo de Comunicação

### Porta de Escuta
```
Porta: 12345
Host: localhost (127.0.0.1)
```

### Fluxo de Comunicação

#### 1. Estabelecimento da Conexão
```java
Socket clienteSocket = new Socket("127.0.0.1", 12345);
```

#### 2. Cliente → Servidor (Requisição)
O cliente envia um objeto `Integer` contendo o ID do grupo (1-10):

```java
ObjectOutputStream saida = new ObjectOutputStream(clienteSocket.getOutputStream());
saida.writeObject(idGrupo); // Integer de 1 a 10
```

#### 3. Processamento no Servidor
1. **Validação:** Verifica se o ID está entre 1 e 10
2. **Incremento:** Aumenta contador do grupo solicitado
3. **Log:** Registra timestamp da operação no formato "yyyy-MM-dd HH:mm:ss"
4. **Thread Safety:** Operações sincronizadas para múltiplos clientes

#### 4. Servidor → Cliente (Resposta)
O servidor envia **DOIS objetos** em sequência:

**Primeiro Objeto - Ranking Completo:**
```java
List<ContadorGrupo> rankingCompleto = new ArrayList<>(contadores);
saida.writeObject(rankingCompleto);
```

**Segundo Objeto - Logs Filtrados:**
```java
List<LogGrupo> logsFiltrados = logs.stream()
    .filter(log -> log.getIdGrupo() == idGrupo)
    .collect(Collectors.toList());
saida.writeObject(logsFiltrados);
```

## Modelos de Dados

### ContadorGrupo
```java
public class ContadorGrupo implements Serializable {
    private int idGrupo;           // ID do grupo (1-10)
    private String nomeGrupo;      // Nome do grupo
    private int quantidadeUtilizacoes; // Contador de utilizações
    
    // Construtores, getters, setters...
}
```

### LogGrupo
```java
public class LogGrupo implements Serializable {
    private int idGrupo;    // ID do grupo (1-10)
    private String timestamp; // Data/hora no formato "yyyy-MM-dd HH:mm:ss"
    
    // Construtores, getters, setters...
}
```

## Como Conectar (Cliente)

### Exemplo de Conexão Básica
```java
try (Socket clienteSocket = new Socket("127.0.0.1", 12345)) {
    
    // 1. Enviar ID do grupo
    ObjectOutputStream saida = new ObjectOutputStream(clienteSocket.getOutputStream());
    saida.writeObject(idGrupo); // Integer de 1 a 10
    
    // 2. Receber dados do servidor
    ObjectInputStream entrada = new ObjectInputStream(clienteSocket.getInputStream());
    
    // 3. Primeiro objeto: Lista completa de contadores
    @SuppressWarnings("unchecked")
    List<ContadorGrupo> ranking = (List<ContadorGrupo>) entrada.readObject();
    
    // 4. Segundo objeto: Logs filtrados do grupo solicitado
    @SuppressWarnings("unchecked")
    List<LogGrupo> logs = (List<LogGrupo>) entrada.readObject();
    
    // 5. Processar dados recebidos...
    
} catch (IOException | ClassNotFoundException e) {
    // Tratamento de erro
}
```

## Comportamento do Servidor

### Thread Safety
- Operações de incremento e logging são sincronizadas
- Suporte a múltiplos clientes simultâneos
- Cada cliente é atendido em thread separada

### Validação de Entrada
```java
if (idGrupo < 1 || idGrupo > 10) {
    System.out.println("ID de grupo inválido: " + idGrupo);
    clienteSocket.close();
    return;
}
```

### Logging do Servidor
O servidor registra no console:
- Inicialização dos grupos
- Conexões de clientes
- IDs de grupos solicitados
- Incrementos de contadores
- Logs adicionados

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

### Saída Esperada
```
Grupos inicializados:
ContadorGrupo{idGrupo=1, nomeGrupo='Financeiro', quantidadeUtilizacoes=0}
ContadorGrupo{idGrupo=2, nomeGrupo='Apostas', quantidadeUtilizacoes=0}
...
Servidor iniciado na porta 12345
Aguardando conexões...
```

## Requisitos Técnicos

- **Java 8+** (para Streams e Collections)
- **Serialização** - Classes implementam `Serializable`
- **Thread Safety** - Uso de `synchronized` blocks
- **Formato de Data** - `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")`

## Considerações de Implementação

1. **Persistência:** Dados são mantidos apenas em memória durante a execução
2. **Concorrência:** Múltiplos clientes podem acessar simultaneamente
3. **Protocolo:** Sempre envia ranking completo + logs filtrados
4. **Validação:** IDs de grupo são validados antes do processamento
5. **Logs:** Cada acesso gera um novo `LogGrupo` com timestamp único 