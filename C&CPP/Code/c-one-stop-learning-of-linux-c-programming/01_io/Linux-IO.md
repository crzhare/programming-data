# Linux I/O

- 所有 I/O 操作最终都是在内核中做的。
- 我们用的 C 标准 I/O 库函数最终也是通过系统调用把 I/O 操作从用户空间传给内核，然后让内核去做 I/O 操作。

## C 标准 I/O 库函数与 Unbuffered I/O 函数

`open、read、write、close`等系统函数称为无缓冲 I/O（Unbuffered I/O）函数，因为它们位于 C 标准库的 I/O 缓冲区的底层。用户程序在读写文件时既可以调用 C 标准 I/O 库函数，也可以直接调用底层的 Unbuffered I/O 函数，如何选择:

- 用 Unbuffered I/O 函数每次读写都要进内核，调一个系统调用比调一个用户空间的函数要慢很多，所以在用户空间开辟 I/O 缓冲区还是必要的，用 C 标准 I/O 库函数就比较方便，省去了自己管理 I/O 缓冲区的麻烦。
- 用 C 标准 I/O 库函数要时刻注意 I/O 缓冲区和实际文件有可能不一致，在必要时需调用 fflush。
- UNIX 的传统是 Everything is a file，I/O 函数不仅用于读写常规文件，也用于读写设备，比如终端或网络设备。在读写设备时通常是不希望有缓冲的，例如向代表网络设备的文件写数据就是希望数据通过网络设备发送出去，而不希望只写到缓冲区里就算完事儿了，当网络设备接收到数据时应用程序也希望第一时间被通知到，所以网络编程通常直接调用 Unbuffered I/O 函数。

C 标准库函数是 C 标准的一部分，**而 Unbuffered I/O 函数是 UNIX 标准的一部分**，在所有支持 C 语言的平台上应该都可以用 C 标准库函数（除了有些平台的 C 编译器没有完全符合 C 标准之外），而只有在 UNIX 平台上才能使用 Unbuffered I/O 函数，所以 C 标 I/O 库函数在头文件 stdio.h 中声明，而 read、write 等函数在头文件 unistd.h 中声明。在支持 C 语言的非 UNIX 操作系统上，标准 I/O 库的底层可能由另外一组系统函数支持，例如 Windows 系统的底层是 Win32 API，其中读写文件的系统函数是 ReadFile、WriteFile。

>虽然 write 系统调用位于 C 标准库 I/O缓冲区的底层，但在 write 的底层也可以分配一个内核 I/O 缓冲区，所以 write 也不一定是直接写到文件的，也可能写到内核 I/O 缓冲区中，至于究竟写到了文件中还是内核缓冲区中对于进程来说是没有差别的，如果进程 A 和进程 B 打开同一文件，进程 A 写到内核 I/O 缓冲区中的数据从进程 B 也能读到，而 C 标准库的 I/O 缓冲区则不具有这一特性。

### POSIX C 标准（UNIX 标准）

POSIX（Portable Operating System Interface）是由 IEEE 制定的标准，致力于统一各种 UNIX 系统的接口，促进各种 UNIX 系统向互相兼容的发向发展。具体参考：

- [C标准I/O库函数与Unbuffered I/O函数](https://akaedu.github.io/book/ch28s02.html)
- [POSIX 标准](https://i.linuxtoy.org/docs/guide/ch48s05.html)

### 文件描述符

每个进程在 Linux 内核中都有一个 task_struct 结构体来维护进程相关的信息，称为**进程描述符（Process Descriptor）**，而在操作系统理论中称为进程控制块（PCB，Process Control Block）。task_struct 中有一个指针指向 files_struct 结构体，称为文件描述符表，其中每个表项包含一个指向已打开的文件的指针。

在 Linux 操作系统中，所有的外围设备（包括键盘和显示器）都被看作是文件系统中的文件，因此，所有的输入/输出都要通过读文件或写文件完成。也就是说，通过一个单一的接口就可以处理外围设备和程序之间的所有通信。

通常情况下，在读或写文件之前，必须先将这个意图通知系统，该过程称为**打开文件**。如果是写一个文件，则可能需要先创建该文件，也可能需要丢弃该文件中原先己存在的内容。 系统会检查你的权限（该文件是否存在？是否有访问它的权限？)，如果一切正常，操作系统将向程序返回一个小的非负整数，该整数称为**文件描述符**。

任何时候对文件的输入/输出都是通过文件描述符标识文件，而不是通过文件名标识文件。（文件描述符类似于标准库中的文件指针或 MS-DOS 中的文件句柄。）系统负责维护已打开文件的所有信息，用户程序只能通过文件描述符引用文件， 因为大多数的输入/输出是通过键盘和显示器来实现的，为了方便起见，UNIX 对此做了特别的安排。当命令解释程序（即"shell"）运行一个程序的时候，它将们打开 3 个文件，对应的文件描述符分别为 0, 1, 2，依次表示标准输入，标准输出和标准错误。如果程序从文件 0 中读，对 1 和 2 进行写，就可以进行输/输出而不必关心打开文件的问题。

头文件 unistd.h 中有如下的宏定义来表示这三个文件描述符：

```c
#define STDIN_FILENO 0
#define STDOUT_FILENO 1
#define STDERR_FILENO 2
```

程序的使用者可通过 `<` 和 `>` 重定程序的 I/O。

```shell
fd < 输入文件名 > 输出文件名
```

这种情况，shell 把文件描述符 0 和 1 的默认赋值改变为指定的文件。通常，文件描述符 2 仍与显示器相关联，这样，出错信息会输出到显示器上。与管道相关的输入/输出也有类似的特性。在任何情况下，文件赋值的改变都不是由程序完成的，而是由 shell 完成的。只要程序使用文件 0 作为输入，文件 1 和 2 作为输出，它就不会知道程序的输入从哪里来，并输出到哪里去。

## open、close、create、unlink 函数

- `create` 用于创建一个文件。
- `unlink(char *name)` 用于将文件 name 从文件系统中删除，它对应的标准库函数是 remove。
- `open`：打开或创建一个文件。
- `close`：关闭一个文件。
- 被创建文件的权限由 open 函数的参数和当前进程 umask 掩码共同决定。

具体参考[Linux C编程一站式学习：第 28 章 文件与I/O-open/close](https://akaedu.github.io/book/ch28s03.html)

## read/write

- read 函数从打开的设备或文件中读取数据。
- write 函数向打开的设备或文件中写数据。
- 非阻塞 poll 实现

具体参考[Linux C编程一站式学习：第 28 章 文件与I/O-read/write](https://akaedu.github.io/book/ch28s04.html)

## lseek

每一个已打开的文件都有一个读写位置, 当打开文件时通常其读写位置是指向文件开头, 若是以附加的方式打开文件(如 O_APPEND), 则读写位置会指向文件尾。 当 read() 或 write() 时, 读写位置会随之增加，lseek() 便是用来控制该文件的读写位置。

具体参考[Linux C编程一站式学习：第 28 章 文件与I/O-lseek](https://akaedu.github.io/book/ch28s05.html)

## fcntl

用 fcntl 函数改变一个已打开的文件的属性，可以重新设置读、写、追加、非阻塞等标志（这些标志称为File Status Flag），而不必重新open文件。

具体参考[Linux C编程一站式学习：第 28 章 文件与I/O-fcntl](https://akaedu.github.io/book/ch28s06.html)

## ioctl

ioctl用于向设备发控制和配置命令，有些命令也需要读写一些数据，但这些数据是不能用read/write读写的，称为Out-of-band数据。也就是说，read/write读写的数据是in-band数据，是I/O操作的主体，而ioctl命令传送的是控制信息，其中的数据是辅助的数据。例如，在串口线上收发数据通过read/write操作，而串口的波特率、校验位、停止位通过ioctl设置，A/D转换的结果通过read读取，而A/D转换的精度和工作频率通过ioctl设置。

具体参考[Linux C编程一站式学习：第 28 章 文件与I/O-ioctl](https://akaedu.github.io/book/ch28s07.html)

## mmap

mmap可以把磁盘文件的一部分直接映射到内存，这样文件中的位置直接就有对应的内存地址，对文件的读写可以直接用指针来做而不需要read/write函数。

具体参考:

- [Linux C编程一站式学习：第 28 章 文件与I/O-mmap](https://akaedu.github.io/book/ch28s08.html)
- [mmap详解](https://nieyong.github.io/wiki_cpu/mmap%E8%AF%A6%E8%A7%A3.html)

## 引用

- [Linux C编程一站式学习：第 28 章 文件与I/O](https://akaedu.github.io/book/ch28.html)
