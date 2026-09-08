TYPST ?= typst

DOCS_DIR := docs
REPORT_SRC := $(DOCS_DIR)/report.typ
REPORT_PDF := $(DOCS_DIR)/report.pdf

REPORT_DEPS := \
	$(REPORT_SRC) \
	$(DOCS_DIR)/title.typ \
	$(DOCS_DIR)/logo.png \
	$(DOCS_DIR)/swagger-workers.png \
	$(DOCS_DIR)/swagger-hr.png

.PHONY: all docs watch clean

all: docs

docs: $(REPORT_PDF)

$(REPORT_PDF): $(REPORT_DEPS)
	$(TYPST) compile --root $(DOCS_DIR) $(REPORT_SRC) $(REPORT_PDF)

watch:
	$(TYPST) watch --root $(DOCS_DIR) $(REPORT_SRC) $(REPORT_PDF)

clean:
	rm -f $(REPORT_PDF)
