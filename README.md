# PDF Chatbot using Spring AI

A simple PDF chatbot application built with Spring AI that allows users to upload PDF documents and ask questions about their content using Retrieval Augmented Generation (RAG).

## Features

- **PDF Upload**: Upload PDF files through a simple web interface
- **Text Extraction**: Extract text from PDFs using Apache PDFBox
- **Text Processing**: Clean and chunk text into manageable pieces
- **Vector Embeddings**: Convert text chunks to vectors using local ONNX models
- **Vector Storage**: Store embeddings in PostgreSQL with pgvector extension
- **RAG Chat**: Ask questions and get answers based on PDF content
- **Simple UI**: Clean, responsive web interface
- **Local Models**: Uses ONNX models locally (no external API calls needed)

## Architecture

The application follows a 5-step PDF processing pipeline:

1. **Extract** → Text extracted using Apache PDFBox
2. **Clean** → Remove extra whitespace and format text
3. **Chunk** → Split into 500-character chunks with overlap
4. **Embed** → Convert each chunk to vector using local ONNX model
5. **Store** → Save chunks and embeddings to PostgreSQL pgvector

## Prerequisites

- Java 21 or higher
- PostgreSQL 12 or higher with pgvector extension
- ONNX models (included in `onnx-output-folder/`)

## Setup Instructions

### 1. Database Setup

1. Install PostgreSQL and the pgvector extension:
   ```bash
   # For Ubuntu/Debian
   sudo apt-get install postgresql postgresql-contrib
   
   # Install pgvector extension
   # Follow instructions at: https://github.com/pgvector/pgvector
   ```

2. Create a database:
   ```sql
   CREATE DATABASE pdfchatbot;
   CREATE EXTENSION vector;
   ```

3. Create a user (optional):
   ```sql
   CREATE USER pdfuser WITH PASSWORD 'password';
   GRANT ALL PRIVILEGES ON DATABASE pdfchatbot TO pdfuser;
   ```

### 2. Application Configuration

1. Update `src/main/resources/application.properties` with your database credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/pdfchatbot
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

2. Verify ONNX model paths (should already be configured):
   ```properties
   spring.ai.transformers.embedding.model-path=file://./onnx-output-folder/model.onnx
   spring.ai.transformers.embedding.tokenizer-path=file://./onnx-output-folder/tokenizer.json
   ```

### 3. Build and Run

1. Build the application:
   ```bash
   ./gradlew build
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```

3. Access the application at: `http://localhost:8080`

## Usage

1. **Upload PDF**: Click "Choose PDF File" and select a PDF document
2. **Wait for Processing**: The application will extract, process, and store the PDF content
3. **Ask Questions**: Once uploaded, you can ask questions about the PDF content
4. **Get Answers**: The chatbot will provide answers based on the PDF content using RAG

## API Endpoints

- `POST /api/upload` - Upload and process a PDF file
- `POST /api/chat` - Send a chat message and get a response
- `GET /api/health` - Health check endpoint
- `DELETE /api/clear` - Clear all data from vector store

## Project Structure

```
src/main/java/com/example/PDF/chatbot/using/Spring/AI/
├── PdfChatbotUsingSpringAiApplication.java  # Main application class
├── config/
│   ├── ApplicationConfig.java               # Application configuration
│   └── OnnxConfig.java                     # ONNX model configuration
├── controller/
│   └── PdfChatController.java               # REST API endpoints
└── service/
    ├── PdfProcessingService.java            # PDF processing logic
    ├── ChatService.java                     # Chat and RAG functionality
    └── OnnxModelTestService.java            # Model testing service
```

## Technologies Used

- **Spring Boot 3.5.4** - Application framework
- **Spring AI 1.0.1** - AI/ML integration
- **Apache PDFBox** - PDF text extraction
- **PostgreSQL** - Database
- **pgvector** - Vector storage extension
- **ONNX Transformers** - Local embedding and chat models
- **Thymeleaf** - Template engine
- **Gradle** - Build tool

## Configuration Options

### Vector Store Configuration
- `spring.ai.vectorstore.pgvector.dimension=384` - Vector dimension
- `spring.ai.vectorstore.pgvector.host=localhost` - Database host
- `spring.ai.vectorstore.pgvector.port=5432` - Database port

### ONNX Model Configuration
- `spring.ai.transformers.embedding.model-path` - Path to ONNX model file
- `spring.ai.transformers.embedding.tokenizer-path` - Path to tokenizer file
- `spring.ai.transformers.embedding.vocab-path` - Path to vocabulary file

### File Upload Configuration
- `spring.servlet.multipart.max-file-size=10MB` - Maximum file size
- `spring.servlet.multipart.max-request-size=10MB` - Maximum request size

## Troubleshooting

### Common Issues

1. **Database Connection Error**:
   - Ensure PostgreSQL is running
   - Check database credentials in `application.properties`
   - Verify pgvector extension is installed

2. **PDF Processing Error**:
   - Ensure the PDF file is not corrupted
   - Check file size limits
   - Verify PDF contains extractable text

3. **ONNX Model Loading Error**:
   - Ensure ONNX model files exist in `onnx-output-folder/`
   - Check file paths in `application.properties`
   - Verify model files are not corrupted

### Logs

Enable debug logging by adding to `application.properties`:
```properties
logging.level.com.example.PDF.chatbot=DEBUG
logging.level.org.springframework.ai=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Acknowledgments

- Spring AI team for the excellent framework
- Apache PDFBox for PDF processing
- ONNX Runtime for local model inference
- PostgreSQL and pgvector communities 