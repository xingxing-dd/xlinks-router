import { defineComponent, h } from "vue";
import { 
  ToastRoot, 
  ToastProvider,
  ToastViewport as RadixToastViewport, 
  ToastTitle as RadixToastTitle, 
  ToastDescription as RadixToastDescription, 
  ToastClose as RadixToastClose, 
  ToastAction as RadixToastAction 
} from "radix-vue";
import { cn } from "@/utils/cn";
import { cva } from "class-variance-authority";
import { X } from "lucide-vue-next";

const toastVariants = cva(
  "group pointer-events-auto relative flex w-full items-center justify-between space-x-4 overflow-hidden rounded-md border p-6 pr-8 shadow-lg transition-all data-[swipe=cancel]:translate-x-0 data-[swipe=end]:translate-x-[var(--radix-toast-swipe-end-x)] data-[swipe=move]:translate-x-[var(--radix-toast-swipe-move-x)] data-[swipe=move]:transition-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[swipe=end]:animate-out data-[state=closed]:fade-out-80 data-[state=closed]:slide-out-to-right-full data-[state=open]:slide-in-from-top-full data-[state=open]:sm:slide-in-from-bottom-full",
  {
    variants: {
      variant: {
        default: "border-bg bg-background text-foreground",
        destructive:
          "destructive group border-destructive bg-destructive text-destructive-foreground",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

const Toast = defineComponent({
  props: ["class", "variant"],
  setup(props, { slots }) {
    return () => h(ToastRoot, { class: cn(toastVariants({ variant: props.variant }), props.class) }, slots);
  },
});

const ToastViewport = defineComponent({
  props: ["class"],
  setup(props, { slots }) {
    return () => h(RadixToastViewport, { class: cn("fixed top-0 z-[100] flex max-h-screen w-full flex-col-reverse p-4 sm:bottom-0 sm:right-0 sm:top-auto sm:flex-col md:max-w-[420px]", props.class) }, slots);
  },
});

const ToastTitle = defineComponent({
  props: ["class"],
  setup(props, { slots }) {
    return () => h(RadixToastTitle, { class: cn("text-sm font-semibold", props.class) }, slots);
  },
});

const ToastDescription = defineComponent({
  props: ["class"],
  setup(props, { slots }) {
    return () => h(RadixToastDescription, { class: cn("text-sm opacity-90", props.class) }, slots);
  },
});

const ToastClose = defineComponent({
  props: ["class"],
  setup(props, { slots }) {
    return () => h(RadixToastClose, { class: cn("absolute right-2 top-2 rounded-md p-1 text-foreground/50 opacity-0 transition-opacity hover:text-foreground focus:opacity-100 focus:outline-none focus:ring-2 group-hover:opacity-100", props.class), "toast-close": "" }, { default: () => h(X, { class: "h-4 w-4" }) });
  },
});

const ToastAction = defineComponent({
  props: ["class"],
  setup(props, { slots }) {
    return () => h(RadixToastAction, { class: cn("inline-flex h-8 shrink-0 items-center justify-center rounded-md border bg-transparent px-3 text-sm font-medium ring-offset-background transition-colors hover:bg-secondary focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 group-[.destructive]:border-muted/40 group-[.destructive]:hover:border-destructive/30 group-[.destructive]:hover:bg-destructive group-[.destructive]:hover:text-destructive-foreground group-[.destructive]:focus:ring-destructive", props.class) }, slots);
  },
});

export { Toast, ToastViewport, ToastTitle, ToastDescription, ToastClose, ToastAction, ToastProvider };
