from struct import pack


def gamma(n):
    """Generate gamma code for a number."""
    binary = format(n, 'b')
    offset = binary[1:]
    length = '1' * len(offset) + '0'
    bits_representation = '{}{}'.format(length, offset)
    return int(bits_representation[::-1], base=2)


def delta(n):
    """Generate delta code for a number."""
    binary = format(n, 'b')
    length = gamma(len(binary))
    offset = binary[1:]
    bits_representation = '{}{}'.format(length, offset)
    return int(bits_representation[::-1], base=2)


def write_string_as_bytes(file, s):
    """Helper function to write a string as bytes to a file.

    Args
    ----
    file: file object (handler)
    s: python string

    Returns
    -------
    Number of characters written
    """
    return file.write(pack('{}s'.format(len(s)), s.encode('ascii')))
