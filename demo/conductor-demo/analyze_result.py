
import sys
if __name__ == '__main__':
    source = sys.argv[1]

    int_array = []
    with open(source, "r") as f:
        lines = f.read().splitlines()
        for line in lines:
            int_array.append(int(line))
    int_array.sort()
    int_array = int_array[1000:]
    length = len(int_array)
    p_99 = int_array[int(length * 99 / 100)]
    p_95 = int_array[int(length * 95 / 100)]
    p_80 = int_array[int(length * 80 / 100)]
    p_50 = int_array[int(length * 50 / 100)]

    print("p99 : ", p_99, "\n")
    print("p95 : ", p_95, "\n")
    print("p80 : ", p_80, "\n")
    print("p50 : ", p_50, "\n")