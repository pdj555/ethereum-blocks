"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import type { Dataset, SearchMode } from "@/lib/types";
import { looksLikeAddress, shorten } from "@/lib/utils";

const DEFAULT_BLOCK = "15049311";
const MAX_SUGGESTIONS = 6;

type CommandDockProps = {
  mode: SearchMode;
  query: string;
  dataset: Dataset | null;
  sticky: boolean;
  onModeChange: (mode: SearchMode) => void;
  onQueryChange: (query: string) => void;
  onSubmit: (query: string) => void;
};

export function CommandDock({
  mode,
  query,
  dataset,
  sticky,
  onModeChange,
  onQueryChange,
  onSubmit
}: CommandDockProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);

  const suggestions = useMemo(() => {
    if (!dataset || !query.trim()) {
      return [];
    }
    const needle = query.trim().toLowerCase();

    if (mode === "block") {
      return dataset.blockNumbers
        .filter((block) => String(block).startsWith(needle))
        .slice(0, MAX_SUGGESTIONS)
        .map((block) => ({
          mode: "block" as const,
          value: String(block),
          label: `Block ${block}`,
          meta: `${dataset.blockMap.get(block)?.parsedTransactionCount ?? 0} parsed tx`
        }));
    }

    if (needle.length < 3) {
      return [];
    }

    return Array.from(dataset.addressMap.keys())
      .filter((address) => address.includes(needle))
      .slice(0, MAX_SUGGESTIONS)
      .map((address) => ({
        mode: "address" as const,
        value: address,
        label: shorten(address),
        meta: dataset.addressMap.get(address)?.behaviorClass.replace("_", " ") ?? ""
      }));
  }, [dataset, mode, query]);

  useEffect(() => {
    setActiveIndex(0);
    setOpen(suggestions.length > 0);
  }, [suggestions]);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      const inField =
        target?.tagName === "INPUT" || target?.tagName === "TEXTAREA" || target?.isContentEditable;

      if ((event.key === "/" || (event.key === "k" && (event.metaKey || event.ctrlKey))) && !inField) {
        event.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
      }

      if (inField && target !== inputRef.current) {
        return;
      }

      if (!inField && event.key === "1") {
        event.preventDefault();
        onModeChange("block");
      }
      if (!inField && event.key === "2") {
        event.preventDefault();
        onModeChange("address");
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onModeChange]);

  function pickSuggestion(value: string, suggestionMode: SearchMode) {
    onModeChange(suggestionMode);
    onQueryChange(value);
    setOpen(false);
    onSubmit(value);
  }

  function handleInputKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (!open || suggestions.length === 0) {
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((index) => (index + 1) % suggestions.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((index) => (index - 1 + suggestions.length) % suggestions.length);
    } else if (event.key === "Enter" && suggestions[activeIndex]) {
      event.preventDefault();
      const pick = suggestions[activeIndex];
      pickSuggestion(pick.value, pick.mode);
    } else if (event.key === "Escape") {
      setOpen(false);
    }
  }

  const hint =
    mode === "block"
      ? "Type a block number, scrub the timeline, or press / to focus."
      : "Paste a full address — suggestions appear after three characters.";

  return (
    <div className={`command-dock${sticky ? " is-sticky" : ""}`}>
      <form
        className="search-bar"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit(query);
          setOpen(false);
        }}
        noValidate
      >
        <div className="mode-switch" aria-label="Search type">
          {(["block", "address"] as const).map((item) => (
            <button
              key={item}
              type="button"
              className={`mode-switch__item${mode === item ? " is-active" : ""}`}
              aria-pressed={mode === item}
              onClick={() => onModeChange(item)}
            >
              {item === "block" ? "Block" : "Address"}
            </button>
          ))}
        </div>
        <label className="sr-only" htmlFor="query-input">
          Search query
        </label>
        <div className="search-bar__field">
          <input
            ref={inputRef}
            id="query-input"
            className="search-bar__input"
            name="query"
            type="text"
            inputMode="search"
            autoComplete="off"
            spellCheck={false}
            value={query}
            placeholder={
              mode === "block" ? DEFAULT_BLOCK : "0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f"
            }
            onChange={(event) => onQueryChange(event.target.value)}
            onFocus={() => setOpen(suggestions.length > 0)}
            onBlur={() => window.setTimeout(() => setOpen(false), 120)}
            onKeyDown={handleInputKeyDown}
          />
          {open ? (
            <ul className="suggest-list" role="listbox">
              {suggestions.map((item, index) => (
                <li key={`${item.mode}-${item.value}`} role="presentation">
                  <button
                    type="button"
                    role="option"
                    aria-selected={index === activeIndex}
                    className={`suggest-item${index === activeIndex ? " is-active" : ""}`}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => pickSuggestion(item.value, item.mode)}
                  >
                    <span>{item.label}</span>
                    <span>{item.meta}</span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
        </div>
        <button className="search-bar__submit" type="submit">
          Inspect
        </button>
      </form>
      <p className="search-hint">
        {hint}
        <span className="search-hint__keys">
          {" · "}
          <kbd>1</kbd> block · <kbd>2</kbd> address · <kbd>/</kbd> focus
        </span>
      </p>
    </div>
  );
}

export function isBlockQuery(value: string): boolean {
  return /^\d+$/.test(value.trim());
}

export function normalizeAddressQuery(value: string): string | null {
  const normalized = value.trim().toLowerCase();
  return looksLikeAddress(normalized) ? normalized : null;
}
