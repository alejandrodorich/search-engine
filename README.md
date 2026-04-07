# Lima-Labs Search Engine


## Table of contents

- [Lima-Labs Search Engine](#lima-labs-search-engine)
  - [1. Description](#1-description)
  - [2. How it works](#2-how-it-works)
  - [3. Project Structure](#3-project-structure)
  - [4. Installation](#4-installation)
    - [4.1. Prerequisites](#41-Prerequisites)
    - [4.2. Clone the Repository](#42-clone-the-repository)
    - [4.3. Install Dependencies](#43-install-dependencies)
    - [4.4. Recommended VSCode Extensions](#44-recommended-vscode-extensions)
  - [5. How to Run the Program](#5-how-to-run-the-program)
  - [6. How to Visualize Graphs (optional)](#6-how-to-visualize-graphs-optional)
  

## 1. Description

This project was developed as part of a university course in databases and 
information systems. It implements a crawler and different indexes that allow 
gathering data from HTML websites, as well as efficiently storing and 
performing searches on this data.

> **Note:** The original project used a university-provided intranet. 
> This public version includes a self-contained mock intranet simulating 
> the fictional company Lima-Labs, replacing the original data source.

For a full technical description refer to the 
[Project Documentation](Documentation.pdf).


## 2. How it works

The search engine crawls the Lima-Labs mock intranet, extracts content from each page, and builds three key indexes:


- Basic Forward Index: Maps each URL to its tokens (after tokenization, lemmatization, and stop-word removal).

- Reverse Index: Maps each token to all URLs containing it, with TF-IDF values for relevance-based search.

- Vectorized Forward Index:
  Extends the basic index by mapping each URL to a vector, where each entry represents the TF-IDF value for the URL and 
  one token gathered during the crawling process. Key improvement: The vector includes all tokens from the entire crawl 
  (not just those in the URL), enabling advanced relevance ranking via cosine similarity and PageRank.

Additional Features:

Supports both standard and weighted search queries.
Graph visualization of site references and metrics (TF-IDF, PageRank, relevance factors).

For a detailed technical description of all data structures, core components, 
and test results, refer to the [Project Documentation](Documentation.pdf).


## 3. Project Structure

```markdown
search-engine/
├── intranet/                   # Local mock intranet (Lima-Labs)
│   ├── serve.py                # Python HTTP server
│   ├── pages/                  # HTML pages (customers, internal, investors, suppliers)
│   └── lima-labs-*.json        # JSON seed files for each network
├── src/
│   ├── main/java/              # Application source code
│   └── test/java/              # JUnit test classes
├── figures/                    # Generated graph visualizations (auto-generated on run)
│   └── .gitkeep
├── .gitignore
├── Documentation.pdf           # Full project documentation
└── pom.xml                     # Maven build configuration
```

> **Note:** The `figures/` directory is initially empty. 
> The graph visualizations are generated automatically 
> when the program is executed.


## 4. Installation

### 4.1. Prerequisites

Make sure the following are installed before running the program:

- **Java JDK 21** or higher
- **Maven**
- **Python 3**
- An IDE such as **VSCode** or **IntelliJ** (recommended)

> All Java dependencies (jsoup, Stanford CoreNLP, Log4j, etc.) are managed 
> automatically by Maven and will be downloaded on the first run.

### 4.2. Clone the Repository
```bash
# clone the repository
git clone https://github.com/alejandrodorich/search-engine.git

# Navigate to the Quicksort folder
cd search-engine
```

### 4.3. Install Dependencies
Maven will download all required Java dependencies automatically on the first run.
To download them in advance, run:
```bash
mvn install -DskipTests
```

### 4.4. Recommended VSCode Extensions

If using VSCode or VSCodium, the following extensions are recommended:

| Extension | Description |
| --------- | ----------- |
| *Extension Pack for Java* | Collection of popular extensions for writing, testing and debugging Java in VSCode. |
| *SonarLint* | Detects & fixes coding issues locally in Java and other languages. |
| *Git Graph* | Visualize and manage your Git repository directly in VSCode. |
| *Red Hat Dependency Analytics* | Provides insights on security vulnerabilities in your dependencies. |
| *PlantUML* | Rich PlantUML support for VSCode. |
| *Markdown All in One* | All you need to write Markdown. |
| *Markdownlint* | Markdown linting and style checking. |
| *Markdown Preview Github Styling* | Matches VSCode's markdown preview to GitHub's style. |


## 5. How to Run the Program

The following steps explain how to interact with the LimaLabs Search Engine via the terminal.

### 5.1. Start the Local Server
Before running the program, start the local intranet server.
Open a terminal and run:
```bash
cd "search-engine/intranet/"
python3 serve.py
```
The server will be available at: `http://localhost:8080`

### 5.2. Run the Program

**Option 1: Via IDE (Recommended)**
Navigate to `LimaLabsSearchEngine` and run the class directly.
**Path:** `search-engine/src/main/java/io/github/alejandrodorich/searchengine/`

**Option 2: Via Terminal (Maven)**
```bash
cd "search-engine/"
mvn compile exec:java -Dexec.mainClass="io.github.alejandrodorich.searchengine.LimaLabsSearchEngine"
```
> **Note:** The first run may take longer as Maven downloads required dependencies. Subsequent runs will be significantly faster.

The terminal will display:
- `Starting search-engine for Lima-Labs...`
- `Java awt GraphicEnvironment headless: true`

### 5.3. Choose Your Search Method
After a few seconds, the following prompt will appear:
- `Please choose between a normal or weighted query (Input "normal" or "weighted")`

Click next to the prompt and enter your preferred search method.

### 5.4. Normal Search
If you chose `normal`, the following prompt will be displayed:
- `Search: `

Enter your query. The program will search the entire intranet and display all relevant results, including page titles and URLs, in the terminal.
If no relevant results are found, the terminal will display:
- `No relevant results found`

Afterwards, see [Step 6](#56-continue-or-exit).

### 5.5. Weighted Search
If you chose `weighted`, the following prompts will be displayed:
- `In order to perform a search, you need to input each query token and weight separately.`
- `To finish the search please write "finished" as an input token.`
- `Please enter a search token: `

**Entering a token:**
Enter a single search token.
> **Note:** If you enter more than one token, the following message will appear:
> `Only one token per index of weightedQuery allowed.`
> You will then be prompted to enter a token again.

**Assigning a weight:**
After entering a token, the following prompt will appear:
- `Please assign a weight for the token (from 1 to 10): `

Enter a weight between 1 and 10.
> **Note:**
> - If the value is out of range: `The weight has to be in the range of 1 to 10.`
> - If the value is not a number: `Invalid input. The weight has to be a number between 1 and 10.`
> In either case, you will be prompted to enter the token again.

You can continue adding tokens and weights. To run the search, enter `finished` as the next token.

Afterwards, see [Step 6](#56-continue-or-exit).

### 5.6. Continue or Exit
After completing a search, the following prompt will appear:
- `Do you want to perform more searches? (Enter "y" for yes, any other key to exit)`

- Enter `y` to perform another search and return to [Step 3](#53-choose-your-search-method).
- Enter any other key to exit and terminate the program.


## 6. How to Visualize Graphs (Optional)

This section explains how to generate graphs that visualize the references between crawled sites.

### 6.1. Prerequisites
Before running the program, make sure that the local server is running, as described in [Start the local server](#51-start-the-local-server).

### 6.2. Run the Program

**Option 1: Via IDE (Recommended)**
Navigate to `DirectedGraph` and run the class directly.
**Path:** `search-engine/src/main/java/io/github/alejandrodorich/searchengine/`

**Option 2: Via Terminal (Maven)**
```bash
cd "search-engine/"
mvn compile exec:java -Dexec.mainClass="io.github.alejandrodorich.searchengine.DirectedGraph"
```
> **Note:** The first run may take longer as Maven downloads required dependencies. Subsequent runs will be significantly faster.

The program generates **4 PNG files** in the 'figures/' folder:
- **net-graph.png**: Visualizes the first 8 crawled sites in the Lima-Labs customer network as nodes, with edges representing references.
- **support.png**: Adds TF-IDF values for the query "support" to the nodes in net-graph.png.
- **support_page-rank.png**: Scales nodes by their PageRank values.
- **support_top3.png**: Highlights the top 3 most relevant URLs (colored light green) based on their relevance factor.
