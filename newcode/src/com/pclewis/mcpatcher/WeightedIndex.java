package com.pclewis.mcpatcher;

abstract public class WeightedIndex {
    final int size;

    public static WeightedIndex create(int size) {
        if (size <= 0) {
            return null;
        }

        return new WeightedIndex(size) {
            @Override
            public int choose(long key) {
                return mod(key, size);
            }
        };
    }

    public static WeightedIndex create(int size, final String weightList) {
        if (size <= 0 || weightList == null) {
            return create(size);
        }

        final int[] weights = new int[size];
        int sum1 = 0;
        boolean useWeight = false;
        String[] list = weightList.trim().split("\\s+");
        for (int i = 0; i < size; i++) {
            if (i < list.length && list[i].matches("^\\d+$")) {
                weights[i] = Math.max(Integer.parseInt(list[i]), 0);
            } else {
                weights[i] = 1;
            }
            if (i > 0 && weights[i] != weights[0]) {
                useWeight = true;
            }
            sum1 += weights[i];
        }
        if (!useWeight || sum1 <= 0) {
            return create(size);
        }
        final int sum = sum1;

        return new WeightedIndex(size) {
            @Override
            public int choose(long key) {
                int index;
                int m = mod(key, sum);
                for (index = 0; index < size - 1 && m >= weights[index]; index++) {
                    m -= weights[index];
                }
                return index;
            }
        };
    }
    
    protected WeightedIndex(int size) {
        this.size = size;
    }

    protected final int mod(long n, int modulus) {
        return (int) (((n >> 32) ^ n) & 0x7fffffff) % modulus;
    }

    abstract public int choose(long key);
}
