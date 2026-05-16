// useModelSearch.ts - Custom hook for AI model discovery

import { useState, useEffect, useCallback, useRef } from 'react';
import { ModelSearchResult } from '../components/api-keys/types';

const searchHuggingFace = async (query: string, signal?: AbortSignal): Promise<ModelSearchResult[]> => {
    try {
        const params = new URLSearchParams({
            search: query,
            limit: '20',
            sort: 'likes',
            direction: '-1'
        });
        const response = await fetch(`https://huggingface.co/api/models?${params}`, { signal });
        if (!response.ok) return [];
        const data = await response.json();
        return data.map((m: any) => ({
            id: m.id,
            name: m.id,
            provider: 'huggingface',
            providerTitle: 'HuggingFace',
            baseUrl: 'https://api-inference.huggingface.co',
            description: m.pipeline_tag || 'Machine Learning Model',
            category: m.pipeline_tag || 'General',
            downloads: m.downloads,
            likes: m.likes,
            pipelineTag: m.pipeline_tag,
        }));
    } catch (error) {
        console.warn('HuggingFace search failed:', error);
        return [];
    }
};

const searchOpenRouter = async (query: string, signal?: AbortSignal): Promise<ModelSearchResult[]> => {
    try {
        const response = await fetch('https://openrouter.ai/api/v1/models', { signal });
        if (!response.ok) return [];
        const data = await response.json();
        const models = (data.data || []) as any[];
        const lower = query.toLowerCase();
        const filtered = query
            ? models.filter((m) => m.id.toLowerCase().includes(lower) || m.name.toLowerCase().includes(lower))
            : models;
        return filtered.slice(0, 20).map((m: any) => ({
            id: m.id,
            name: m.name || m.id,
            provider: 'openrouter',
            providerTitle: 'OpenRouter',
            baseUrl: 'https://openrouter.ai/api/v1',
            description: m.description || 'AI Model via OpenRouter',
            category: 'OpenRouter',
        }));
    } catch (error) {
        console.warn('OpenRouter search failed:', error);
        return [];
    }
};

export const useModelSearch = (
    delay: number = 500,
    sources: ('huggingface' | 'openrouter')[] = ['huggingface', 'openrouter']
) => {
    const [results, setResults] = useState<ModelSearchResult[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const timeoutRef = useRef<NodeJS.Timeout | null>(null);
    const abortControllerRef = useRef<AbortController | null>(null);

    const search = useCallback(async (query: string) => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        timeoutRef.current = setTimeout(async () => {
            if (!query.trim()) {
                setResults([]);
                setLoading(false);
                return;
            }

            setLoading(true);
            setError(null);
            abortControllerRef.current = new AbortController();
            const signal = abortControllerRef.current.signal;

            try {
                const promises: Array<Promise<ModelSearchResult[]>> = [];
                if (sources.includes('huggingface')) {
                    promises.push(searchHuggingFace(query, signal));
                }
                if (sources.includes('openrouter')) {
                    promises.push(searchOpenRouter(query, signal));
                }

                const allResults = await Promise.all(promises);
                const flattened = allResults.flat();
                // Deduplicate by id
                const seen = new Set<string>();
                const unique = flattened.filter(m => {
                    if (seen.has(m.id)) return false;
                    seen.add(m.id);
                    return true;
                });
                unique.sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
                setResults(unique.slice(0, 30));
            } catch (err: any) {
                if (err.name !== 'AbortError') {
                    setError('Search failed. Please try again.');
                    setResults([]);
                }
            } finally {
                setLoading(false);
            }
        }, delay);
    }, [sources, delay]);

    useEffect(() => {
        return () => {
            if (timeoutRef.current) clearTimeout(timeoutRef.current);
            if (abortControllerRef.current) abortControllerRef.current.abort();
        };
    }, []);

    return { results, loading, error, search, setResults };
};
