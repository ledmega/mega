# B2B SaaS Chatbot Builder Project

## Phase 1: Planning & Architecture Design
- [x] Initial B2B SaaS Chatbot business model ideation
- [x] Gemini API Key Management design
- [x] Draft system architecture & implementation plan

## Phase 2: Backend & Database Development
- [ ] Design Multi-tenant RDB schema (Tenant, BotConfig, UsageLog)
- [ ] Setup Vector DB (e.g., pgvector, Milvus) for RAG
- [ ] Implement Gemini Proxy API Server in Spring Boot
- [ ] Develop RAG embedding and semantic search pipeline
- [ ] **Develop Web Crawler logic for URL-based knowledge ingestion**
- [ ] **Implement Dynamic Prompt loading from DB per Tenant**
- [ ] **Implement API Usage & Token Logging for Billing**

## Phase 3: Admin Dashboard Development
- [ ] Create tenant registration & API token flow
- [ ] Implement bot customization UI (System Prompt Editor)
- [ ] Implement Knowledge Base upload interface
- [ ] Develop JS Snippet generator

## Phase 4: Frontend Widget Development
- [ ] Build embeddable `chatbot-widget.js` SDK
- [ ] Implement responsive UI for Chat interactions
- [ ] Secure communication with backend APIs
