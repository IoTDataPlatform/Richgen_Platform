SHELL := /bin/bash

DC ?= docker compose
CONNECT_URL ?= http://localhost:8083
CONNECTORS_CONFIGS_DIR ?= ./configs

RICH_SOURCE_NAME ?= rich-simulator-source
RICH_SOURCE_JSON := $(CONNECTORS_CONFIGS_DIR)/$(RICH_SOURCE_NAME).json

define create_connector
	curl -fsS -o /dev/null -w "%{http_code}" -X POST \
	  -H 'Content-Type: application/json' \
	  --data-binary @$(1) \
	  $(CONNECT_URL)/connectors
endef

define delete_connector
	curl -fsS -o /dev/null -w "%{http_code}" -X DELETE \
	  $(CONNECT_URL)/connectors/$(1)
endef

define pause_connector
	curl -fsS -o /dev/null -w "%{http_code}" -X PUT \
	  $(CONNECT_URL)/connectors/$(1)/pause
endef

define resume_connector
	curl -fsS -o /dev/null -w "%{http_code}" -X PUT \
	  $(CONNECT_URL)/connectors/$(1)/resume
endef

define restart_connector
	curl -fsS -o /dev/null -w "%{http_code}" -X POST \
	  $(CONNECT_URL)/connectors/$(1)/restart
endef

.PHONY: up down clean-volumes restart logs ps
up:
	$(DC) up -d

down:
	$(DC) down

clean-volumes:
	$(DC) down -v

restart: down up

logs:
	$(DC) logs -f

ps:
	$(DC) ps

.PHONY: connect-health plugins connectors status
connect-health:
	@bash -c 'for i in {1..60}; do \
	  curl -fsS $(CONNECT_URL)/connectors >/dev/null && echo "Connect is up" && exit 0; \
	  echo "Waiting for Connect... $$i"; sleep 2; done; \
	  echo "Connect is not responding at $(CONNECT_URL)"; exit 1'

plugins:
	@curl -fsS $(CONNECT_URL)/connector-plugins | jq || true

connectors:
	@curl -fsS $(CONNECT_URL)/connectors | jq || true

status:
ifndef NAME
	$(error Use: make status NAME=<connector-name>)
endif
	@curl -fsS $(CONNECT_URL)/connectors/$(NAME)/status || true

.PHONY: enable-rich-source disable-rich-source pause-rich-source resume-rich-source restart-rich-source

enable-rich-source: connect-health
	@code="$$( $(call create_connector,$(RICH_SOURCE_JSON)) )"; \
	if [ "$$code" = "201" ] || [ "$$code" = "409" ]; then \
	  echo "Rich Source enabled (HTTP $$code)"; \
	else echo "Failed to enable Rich Source (HTTP $$code)"; exit 1; fi

disable-rich-source:
	@code="$$( $(call delete_connector,$(RICH_SOURCE_NAME)) )"; \
	if [ "$$code" = "204" ] || [ "$$code" = "404" ]; then \
	  echo "Rich Source disabled (HTTP $$code)"; \
	else echo "Failed to disable Rich Source (HTTP $$code)"; exit 1; fi

pause-rich-source:
	@code="$$( $(call pause_connector,$(RICH_SOURCE_NAME)) )"; echo "Pause Rich Source: HTTP $$code"

resume-rich-source:
	@code="$$( $(call resume_connector,$(RICH_SOURCE_NAME)) )"; echo "Resume Rich Source: HTTP $$code"

restart-rich-source:
	@code="$$( $(call restart_connector,$(RICH_SOURCE_NAME)) )"; echo "Restart Rich Source: HTTP $$code"

.PHONY: help
help:
	@echo "Commands:"
	@echo "  up / down / clean-volumes / restart / logs / ps"
	@echo "  connect-health, plugins, connectors, status NAME=<name>"
	@echo "  enable-rich-source | disable-rich-source | pause-rich-source | resume-rich-source | restart-rich-source"
