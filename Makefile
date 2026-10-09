TYPST ?= typst
PLANTUML ?= plantuml

REPORT_DIR := docs/report
LABS := lab-1 lab-2
LAB ?= lab-1

COMMON_DEPS := $(wildcard $(REPORT_DIR)/common/*)
REPORT_PDFS := $(foreach lab,$(LABS),$(REPORT_DIR)/$(lab)/report.pdf)

.PHONY: all docs watch clean

all: docs

docs: $(REPORT_PDFS)

.SECONDEXPANSION:
$(REPORT_DIR)/%/report.pdf: $$(wildcard $(REPORT_DIR)/$$*/*.typ $(REPORT_DIR)/$$*/*.png) $$(addsuffix .svg,$$(basename $$(wildcard $(REPORT_DIR)/$$*/*.puml))) $(COMMON_DEPS)
	$(TYPST) compile --root $(REPORT_DIR) $(REPORT_DIR)/$*/report.typ $@

%.svg: %.puml
	$(PLANTUML) -tsvg $<

watch:
	$(TYPST) watch --root $(REPORT_DIR) $(REPORT_DIR)/$(LAB)/report.typ $(REPORT_DIR)/$(LAB)/report.pdf

clean:
	rm -f $(REPORT_PDFS)
